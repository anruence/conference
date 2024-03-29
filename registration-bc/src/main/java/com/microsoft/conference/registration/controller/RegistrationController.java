package com.microsoft.conference.registration.controller;

import com.microsoft.conference.common.ActionResult;
import com.microsoft.conference.common.ErrCode;
import com.microsoft.conference.common.payment.commands.CreatePayment;
import com.microsoft.conference.common.registration.commands.order.AssignRegistrantDetails;
import com.microsoft.conference.common.registration.commands.order.MarkAsSuccess;
import com.microsoft.conference.common.registration.commands.order.PlaceOrder;
import com.microsoft.conference.registration.domain.order.model.OrderStatus;
import com.microsoft.conference.registration.readmodel.PayConvert;
import com.microsoft.conference.registration.readmodel.RegistrationConvert;
import com.microsoft.conference.registration.readmodel.service.*;
import org.enodeframework.commanding.*;
import org.enodeframework.common.io.Task;
import org.enodeframework.common.utils.IdGenerator;
import org.enodeframework.queue.SendMessageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.enodeframework.common.io.Task.await;

@RestController
public class RegistrationController {

    public String thirdPartyProcessorPayment = "thirdParty";

    @Autowired
    private CommandBus commandService;
    @Autowired
    private ConferenceQueryService conferenceQueryService;
    @Autowired
    private OrderQueryService orderQueryService;

    @GetMapping("registration/{id}")
    public ActionResult<OrderViewModel> startRegistration(@PathVariable String slug) {
        ConferenceAlias alias = conferenceQueryService.getConferenceAlias(slug);
        return view(createViewModel(alias));
    }

    @PostMapping("registration")
    public ActionResult<String> startRegistration(@RequestBody OrderViewModel model) {
        ConferenceAlias alias = conferenceQueryService.getConferenceAlias(model.getConferenceCode());
        PlaceOrder command = RegistrationConvert.INSTANCE.toPlaceOrderCommand(alias);
        if (!command.getSeatInfos().stream().findAny().isPresent()) {
            String errMsg = "ConferenceCode, You must reservation at least one seat.";
            return ActionResult.error(ErrCode.SYSTEM_ERROR, errMsg);
        }
        CommandResult result = await(executeCommandAsync(command));
        return view(result.getResult());
    }

    @GetMapping("specify")
    public ActionResult<RegistrationViewModel> specifyRegistrantAndPaymentDetails(@RequestParam String orderId) {
        OrderVO order = this.waitUntilReservationCompleted(orderId);
        if (order == null) {
            return ActionResult.error(ErrCode.SYSTEM_ERROR, "PricedOrderUnknown");
        }
        if (order.getStatus() != OrderStatus.ReservationSuccess.getStatus()) {
            return ActionResult.error(ErrCode.SYSTEM_ERROR, "ReservationFailed");
        }
        RegistrationViewModel viewModel = new RegistrationViewModel();
        viewModel.setOrderVO(order);
        RegistrantDetails registrantDetails = new RegistrantDetails();
        registrantDetails.setOrderId(orderId);
        viewModel.setRegistrantDetails(registrantDetails);
        return view(viewModel);
    }

    @PostMapping("specify")
    public ActionResult<String> specifyRegistrantAndPaymentDetails(String orderId, RegistrantDetails viewModel) {
        AssignRegistrantDetails registrantDetails = RegistrationConvert.INSTANCE.toAssignRegistrantDetails(viewModel);
        registrantDetails.setAggregateRootId(orderId);
        await(sendCommandAsync(registrantDetails));
        return this.startPayment(orderId);
    }

    @PostMapping("startpay")
    public ActionResult<String> startPayment(@RequestParam String orderId) {
        OrderVO order = this.orderQueryService.findOrder(orderId);
        if (order == null) {
            return ActionResult.error(ErrCode.SYSTEM_ERROR, "ReservationUnknown");
        }
        if (order.getStatus() == OrderStatus.PaymentSuccess.getStatus() || order.getStatus() == OrderStatus.Success.getStatus()) {
            return view("ShowCompletedOrder");
        }
        if (new Date().before(order.getReservationExpirationDate())) {
            return view("ShowExpiredOrder: " + orderId);
        }
        if (order.isFreeOfCharge()) {
            completeRegistrationWithoutPayment(orderId);
            return view("completeRegistrationWithoutPayment");
        }
        completeRegistrationWithThirdPartyProcessorPayment(order);
        return view("completeRegistrationWithThirdPartyProcessorPayment");
    }


    public ActionResult<OrderVO> thankYou(String orderId) {
        return view(this.orderQueryService.findOrder(orderId));
    }

    private void completeRegistrationWithThirdPartyProcessorPayment(OrderVO order) {
        CreatePayment paymentCommand = createPaymentCommand(order);
        sendCommandAsync(paymentCommand);
    }

    private CreatePayment createPaymentCommand(OrderVO order) {
        ConferenceAlias alias = conferenceQueryService.getConferenceAlias(order.getAccessCode());
        String name = alias.getName();
        String description = "Payment for the order of " + name;
        CreatePayment createPayment = new CreatePayment();
        createPayment.setAggregateRootId(IdGenerator.id());
        createPayment.setConferenceId(alias.getId());
        createPayment.setOrderId(order.getOrderId());
        createPayment.setDescription(description);
        createPayment.setTotalAmount(order.getTotalAmount());
        createPayment.setLines(order.getOrderLines().stream()
                .map(PayConvert.INSTANCE::toPaymentLine)
                .collect(Collectors.toList()));
        return createPayment;
    }

    private void completeRegistrationWithoutPayment(String orderId) {
        Task.await(sendCommandAsync(new MarkAsSuccess(orderId)));
    }

    private OrderViewModel createViewModel(ConferenceAlias alias) {
        // thread local
        List<SeatTypeVO> seatTypes = this.conferenceQueryService.getPublishedSeatTypes(alias.getId());
        OrderViewModel viewModel = new OrderViewModel();
        viewModel.setConferenceCode(alias.getSlug());
        viewModel.setConferenceId(alias.getId());
        viewModel.setConferenceName(alias.getName());
        viewModel.setItems(seatTypes.stream().map(x -> {
            OrderItemViewModel itemViewModel = new OrderItemViewModel();
            itemViewModel.setSeatTypeVO(x);
            itemViewModel.setMaxSelectionQuantity(Math.max(Math.min(x.getAvailableQuantity(), 20), 0));
            return itemViewModel;
        }).collect(Collectors.toList()));
        return viewModel;
    }

    // 轮训订单状态，直到订单的库存预扣操作完成
    private OrderVO waitUntilReservationCompleted(String orderId) {
        OrderVO x = this.orderQueryService.findOrder(orderId);
        if (x != null) {
            if (x.getStatus() == OrderStatus.ReservationSuccess.getStatus()
                    || x.getStatus() == OrderStatus.ReservationFailed.getStatus()) {
                return x;
            }
        }
        return null;
    }

    private CompletableFuture<SendMessageResult> sendCommandAsync(CommandMessage command) {
        return commandService.sendAsync(command);
    }

    private boolean isSuceess(CommandResult result) {
        return CommandStatus.Success.equals(result.getStatus());
    }

    private CompletableFuture<CommandResult> executeCommandAsync(CommandMessage command) {
        return commandService.executeAsync(command, CommandReturnType.CommandExecuted);
    }

    private <T> ActionResult<T> view(T objects) {
        return new ActionResult<>(objects);
    }
}
