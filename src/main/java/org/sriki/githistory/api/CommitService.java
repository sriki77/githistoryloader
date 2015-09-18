package org.sriki.githistory.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sriki.githistory.db.DBHandler;
import org.sriki.githistory.model.Commit;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.Month;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Path("/commits")
@Produces(MediaType.APPLICATION_JSON)
@Singleton
public class CommitService extends AbstractService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommitService.class);
    private final DBHandler dbHandler;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    @Inject
    public CommitService() {
        this.dbHandler = DBHandler.getInstance();
    }

    @GET
    @Path("/date-wise")
    public Response commitsDateWise() {
        List<Commit> commitList = dbHandler.commits();
        Map<String, Long> dateCountMap = commitList.stream().collect(Collectors.groupingBy(
                commit -> dateFormat.format(commit.getMergeTime()), Collectors.counting()));
        return toResponse(dateCountMap);
    }

    @GET
    @Path("/month-wise")
    public Response commitsMonthWise() {
        List<Commit> commitList = dbHandler.commits();
        Map<Month, Long> monthCountMap = commitList.stream().collect(Collectors.groupingBy(
                commit -> commit.getMergeTime().toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate().getMonth(), Collectors.counting()));
        return toResponse(new TreeMap<>(monthCountMap));
    }

    @GET
    @Path("/day-wise")
    public Response commitsDayWise() {
        List<Commit> commitList = dbHandler.commits();
        Map<DayOfWeek, Long> dayCountMap = commitList.stream().collect(Collectors.groupingBy(
                commit -> commit.getMergeTime().toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate().getDayOfWeek(), Collectors.counting()));
        return toResponse(new TreeMap<>(dayCountMap));
    }

    @GET
    @Path("/date-wise-stats")
    public Response commitsDateWiseStats() {
        List<Commit> commitList = dbHandler.commits();
        Map<String, Integer> commitStats = new HashMap<>();
        commitStats.put("total", commitList.size());
        return toResponse(commitStats);
    }


}
