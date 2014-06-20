package org.synyx.urlaubsverwaltung.web.calendar;

import com.google.gson.Gson;

import org.joda.time.DateMidnight;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Controller;

import org.springframework.ui.Model;

import org.springframework.util.StringUtils;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import org.synyx.urlaubsverwaltung.core.application.domain.Application;
import org.synyx.urlaubsverwaltung.core.application.domain.ApplicationStatus;
import org.synyx.urlaubsverwaltung.core.application.domain.DayLength;
import org.synyx.urlaubsverwaltung.core.application.service.ApplicationService;
import org.synyx.urlaubsverwaltung.core.calendar.GoogleCalendarService;
import org.synyx.urlaubsverwaltung.core.calendar.JollydayCalendar;
import org.synyx.urlaubsverwaltung.core.calendar.OwnCalendarService;
import org.synyx.urlaubsverwaltung.core.person.Person;
import org.synyx.urlaubsverwaltung.core.person.PersonService;

import java.io.IOException;

import java.math.BigDecimal;

import java.util.ArrayList;
import java.util.List;


/**
 * TODO: Move these methods in RestAPI
 *
 * <p>Controller for calendar relevant stuff.</p>
 *
 * @author  Aljona Murygina
 */
@Controller
public class CalendarController {

    private static final String JSP_FOLDER = "calendar/";

    private static final String DATE_PATTERN = "yyyy-MM-dd"; // please do not change, because is used in custom.js"

    @Autowired
    private GoogleCalendarService googleCalendarService;

    @Autowired
    private OwnCalendarService ownCalendarService;

    @Autowired
    private JollydayCalendar jollydayCalendar;

    @Autowired
    private PersonService personService;

    @Autowired
    private ApplicationService applicationService;

    /**
     * Is used in application form: ajax call to calculate vacation days for dates given by datepicker.
     *
     * @param  start  start date as String (e.g. 2013-3-21)
     * @param  end  end date as String (e.g. 2013-3-21)
     * @param  length  day length as String (FULL, MORNING or NOON)
     * @param  personId  id of the person to calculate used days for
     *
     * @return  number of days as String for the given parameters or "N/A" if parameters are not valid in any way
     */
    @RequestMapping(value = "/calendar/vacation", method = RequestMethod.GET)
    @ResponseBody
    public String getNumberOfDays(@RequestParam("start") String start,
        @RequestParam("end") String end,
        @RequestParam("length") String length,
        @RequestParam("person") Integer personId) {

        if (StringUtils.hasText(start) && StringUtils.hasText(end) && StringUtils.hasText(length)) {
            DateTimeFormatter fmt = DateTimeFormat.forPattern(DATE_PATTERN);
            DateMidnight startDate = DateMidnight.parse(start, fmt);
            DateMidnight endDate = DateMidnight.parse(end, fmt);

            if (startDate.isBefore(endDate) || startDate.isEqual(endDate)) {
                DayLength howLong = DayLength.valueOf(length);
                Person person = personService.getPersonByID(personId);
                BigDecimal days = ownCalendarService.getWorkDays(howLong, startDate, endDate, person);

                return days.toString();
            }
        }

        return "N/A";
    }


    /**
     * Is used in jquery datepicker to mark public holidays: ajax call to check if date is a public holiday.
     *
     * @param  month
     * @param  year
     *
     * @return  "1" if date is a public holiday, "0" if not, "N/A" if parameter not valid
     */
    @RequestMapping(value = "/calendar/public-holiday", method = RequestMethod.GET)
    @ResponseBody
    public String getPublicHolidays(@RequestParam("year") String year,
        @RequestParam(value = "month", required = false) String month) {

        String response = "N/A";

        boolean hasYear = StringUtils.hasText(year);
        boolean hasMonth = StringUtils.hasText(month);

        try {
            List<String> holidays = null;

            if (hasYear && !hasMonth) {
                holidays = jollydayCalendar.getPublicHolidays(Integer.parseInt(year));
            } else if (hasYear && hasMonth) {
                holidays = jollydayCalendar.getPublicHolidays(Integer.parseInt(year), Integer.parseInt(month));
            }

            response = new Gson().toJson(holidays);
        } catch (NumberFormatException e) {
        }

        return response;
    }


    @RequestMapping(value = "/calendar/holiday", method = RequestMethod.GET)
    @ResponseBody
    public String getPersonsHoliday(@RequestParam("year") String year,
        @RequestParam(value = "month", required = false) String month,
        @RequestParam("person") Integer personId) {

        boolean hasYear = StringUtils.hasText(year);
        boolean hasMonth = StringUtils.hasText(month);

        if (hasYear && personId != null) {
            try {
                Person person = personService.getPersonByID(personId);

                if (person == null) {
                    return "N/A";
                }

                List<Application> applications = hasMonth
                    ? applicationService.getAllAllowedApplicationsOfAPersonForAMonth(person, Integer.parseInt(month),
                        Integer.parseInt(year))
                    : applicationService.getAllApplicationsByPersonAndYearAndState(person, Integer.parseInt(year),
                        ApplicationStatus.ALLOWED);

                List<VacationDate> vacationDateList = new ArrayList<VacationDate>();

                for (Application app : applications) {
                    DateMidnight startDate = app.getStartDate();
                    DateMidnight endDate = app.getEndDate();

                    DateMidnight day = startDate;

                    while (!day.isAfter(endDate)) {
                        vacationDateList.add(new VacationDate(day.toString(DATE_PATTERN), app.getId(),
                                app.getStatus().name()));

                        day = day.plusDays(1);
                    }
                }

                String json = new Gson().toJson(vacationDateList);

                return json;
            } catch (NumberFormatException ex) {
                return "N/A";
            }
        }

        return "N/A";
    }


    /**
     * TODO: google calendar stuff in development Following methods are only for developing...
     */

    @RequestMapping(value = "/calendar", method = RequestMethod.GET)
    public String getCalendarSite(Model model) {

        return JSP_FOLDER + "calendar";
    }


    @RequestMapping(value = "/calendar/setup", method = RequestMethod.GET)
    public String setupGoogleCalendar(Model model) throws IOException {

        googleCalendarService.setUp();

        return JSP_FOLDER + "setup";
    }


    @RequestMapping(value = "/calendar/event", method = RequestMethod.GET)
    public String addEvent(Model model) throws IOException {

        googleCalendarService.addEvent();

        return JSP_FOLDER + "event";
    }

    private class VacationDate {

        private String date;
        private Integer applicationId;
        private String status;

        public VacationDate(String date, Integer applicationId, String status) {

            this.date = date;
            this.applicationId = applicationId;
            this.status = status;
        }

        @Override
        public String toString() {

            return "date = " + this.date + ", href = " + this.applicationId + ", status = " + status;
        }
    }
}