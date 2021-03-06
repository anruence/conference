package com.microsoft.conference.management.controller;

import com.microsoft.conference.common.ActionResult;
import com.microsoft.conference.common.ErrCode;
import com.microsoft.conference.common.management.commands.*;
import com.microsoft.conference.management.domain.model.SeatType;
import com.microsoft.conference.management.readmodel.ConferenceQueryService;
import com.microsoft.conference.management.readmodel.ConferenceVO;
import com.microsoft.conference.management.readmodel.DTOExtensions;
import com.microsoft.conference.management.readmodel.SeatTypeVO;
import com.microsoft.conference.management.request.ConferenceInfo;
import com.microsoft.conference.management.request.EditableConferenceInfo;
import com.microsoft.conference.management.request.LocateConference;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.enodeframework.commanding.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.enodeframework.common.io.Task.await;

@RestController
@RequestMapping("/api/v1/")
@Tag(name = "后台会议管理", description = "conference-management")
public class ConferenceController {

    @Autowired
    private CommandBus commandService;

    @Autowired
    private ConferenceQueryService conferenceQueryService;

    @GetMapping("locate")
    public ActionResult<ConferenceVO> locate(@RequestBody LocateConference locateConference) {
        ConferenceVO conference = conferenceQueryService.findConference(locateConference.getEmail(), locateConference.getAccessCode());
        if (conference == null) {
            String errMsg = "Could not locate a conference with the provided email and access code";
            return ActionResult.error(ErrCode.SYSTEM_ERROR, errMsg);
        }
        return view(conference);
    }

    @PostMapping("conference")
    public ActionResult<String> create(@RequestBody ConferenceInfo conference) {
        CreateConference command = DTOExtensions.INSTANCE.toCreateConferenceCommand(conference);
        CommandResult result = await(executeCommandAsync(command));
        if (!isSuceess(result)) {
            return ActionResult.error(ErrCode.SYSTEM_ERROR, result.getResult());
        }
        return view(result.getResult());
    }

    @PutMapping("conference")
    public ActionResult<String> edit(@RequestBody EditableConferenceInfo conference) {
        UpdateConference command = DTOExtensions.INSTANCE.toUpdateConferenceCommand(conference);
        CommandResult result = await(executeCommandAsync(command));
        if (!isSuceess(result)) {
            return ActionResult.error(ErrCode.SYSTEM_ERROR, result.getResult());
        }
        return view(result.getResult());
    }

    @PostMapping("publish/{id}")
    public ActionResult<String> publish(@PathVariable String id) {
        PublishConference command = new PublishConference();
        command.setAggregateRootId(id);
        CommandResult result = await(executeCommandAsync(command));
        if (!isSuceess(result)) {
            return ActionResult.error(ErrCode.SYSTEM_ERROR, result.getResult());
        }
        return view(result.getResult());
    }

    @PostMapping("unpublish/{id}")
    public ActionResult<String> unpublish(@PathVariable String id) {
        UnpublishConference command = new UnpublishConference();
        command.setAggregateRootId(id);
        CommandResult result = await(executeCommandAsync(command));
        if (!isSuceess(result)) {
            return ActionResult.error(ErrCode.SYSTEM_ERROR, result.getResult());
        }
        return view(result.getResult());
    }

    @GetMapping("seattypes")
    public ActionResult<List<SeatTypeVO>> seatTypes(@RequestParam String conferenceId) {
        List<SeatTypeVO> seatTypes = this.conferenceQueryService.findSeatTypes(conferenceId);
        return view(seatTypes);
    }

    @GetMapping("seattype/{id}")
    public ActionResult<SeatTypeVO> seatRow(@PathVariable("id") String id) {
        SeatTypeVO seatTypeDTO = this.conferenceQueryService.findSeatType(id);
        return view(seatTypeDTO);
    }

    @PostMapping("seattype")
    public ActionResult<String> createSeat(@RequestBody SeatType seat) {
        ConferenceInfo conferenceInfo = new ConferenceInfo();
        AddSeatType command = DTOExtensions.INSTANCE.toAddSeatTypeCommand(seat, conferenceInfo);
        CommandResult result = await(executeCommandAsync(command));
        if (!isSuceess(result)) {
            return ActionResult.error(ErrCode.SYSTEM_ERROR, result.getResult());
        }
        return view(result.getResult());
    }

    @PutMapping("seattype")
    public ActionResult<String> editSeat(@RequestBody SeatType seat) {
        ConferenceInfo conferenceInfo = new ConferenceInfo();
        UpdateSeatType command = DTOExtensions.INSTANCE.toUpdateSeatTypeCommand(seat, conferenceInfo);
        CommandResult result = await(executeCommandAsync(command));
        if (!isSuceess(result)) {
            return ActionResult.error(ErrCode.SYSTEM_ERROR, result.getResult());
        }
        return view(result.getResult());
    }

    @DeleteMapping("seattype/{id}")
    public ActionResult<String> deleteSeat(@PathVariable("id") String id) {
        RemoveSeatType command = new RemoveSeatType(id);
        command.setSeatTypeId(id);
        CommandResult result = await(executeCommandAsync(command));
        if (!isSuceess(result)) {
            return ActionResult.error(ErrCode.SYSTEM_ERROR, result.getResult());
        }
        return view(result.getResult());
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