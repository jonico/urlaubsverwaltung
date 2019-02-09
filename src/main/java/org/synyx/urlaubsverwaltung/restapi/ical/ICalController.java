package org.synyx.urlaubsverwaltung.restapi.ical;

import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.synyx.urlaubsverwaltung.core.ical.ICalService;

import javax.servlet.http.HttpServletResponse;


@Api(value = "iCal", description = "Get iCal")
@Controller
public class ICalController {

    private final ICalService iCalService;

    @Autowired
    public ICalController(ICalService iCalService) {

        this.iCalService = iCalService;
    }

    @GetMapping("/ical")
    @ResponseBody
    public String plaintext(HttpServletResponse response) {

        response.setContentType("text/calendar");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "inline; filename=calendar.ics");

        return iCalService.getICal();
    }
}
