package org.sriki.githistory.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sriki.githistory.LoaderProperties;
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
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Path("/commits")
@Produces(MediaType.APPLICATION_JSON)
@Singleton
public class CommitService extends AbstractService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommitService.class);
    public static final String TEAM_NAME_PROPERTY = "loader.team";
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
        Map<String, Long> dateCountMap = commitList.stream()
                .collect(Collectors.groupingBy(this::localDate,
                        Collectors.counting()));

        Map<String, List<Commit>> dateTagMap
                = commitList.stream().filter(c -> c.getTag() != null).
                collect(Collectors.groupingBy(this::localDate, Collectors.toList()));
        Map<String, String> dateCountTagMap = new TreeMap<>();
        dateCountMap.forEach((k, v) -> {
            List<Commit> commit = dateTagMap.get(k);
            dateCountTagMap.put(k, commit == null ? "" + v : v + ":" + toTagString(commit));
        });
        return toResponse(dateCountTagMap);
    }

    private String toTagString(List<Commit> commit) {
        return commit.stream().map(c -> c.getTag().replace("refs/tags/", "")).collect(Collectors.joining(","));
    }

    private String localDate(Commit c) {
        String format = c.getMergeTime().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
        return format;
    }


    @GET
    @Path("/project-wise")
    public Response commitsProjectWise() {
        List<Commit> commitList = dbHandler.commits();
        Map<String, Long> projectCountMap = commitList.stream().collect(Collectors.groupingBy(
                commit -> commit.getProject().toUpperCase(), TreeMap::new, Collectors.counting()));
        return toResponse(new TreeMap<>(projectCountMap));
    }

    @GET
    @Path("/size-wise")
    public Response commitsSizeWise() {
        List<Commit> commitList = dbHandler.commits();
        Map<String, Integer> commitSizeMap = new HashMap<>();
        for (Commit commit : commitList) {
            String id = String.format("%s-%s", commit.getProject(), commit.getTickerNum());
            commitSizeMap.putIfAbsent(id, 0);
            commitSizeMap.put(id, commitSizeMap.get(id) + commit.getFileCount());
        }
        return toResponse(new TreeMap<>(commitSizeMap));
    }

    @GET
    @Path("/time-wise")
    public Response commitsDayTimeWise() {
        List<Commit> commitList = dbHandler.commits();
        int[] years = commitList.stream().mapToInt(c -> c.getMergeTime().getYear()).distinct().toArray();
        ArrayList<String[]> dayTime = new ArrayList<>();
        HashMap<String, Integer> countMap = new HashMap<>();
        String parent = "Commit Distribution By Calendar (Year,Month,Day,Hour)";
        dayTime.add(new String[]{parent, null, "0"});
        for (int year : years) {
            dayTime.add(new String[]{"" + year, parent, "0"});
            for (Month month : Month.values()) {
                String monthYear = String.format("%s,%s", month.name(), year);
                dayTime.add(new String[]{monthYear, parent, "0"});
                for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
                    dayTime.add(new String[]{String.format("%sS,%s,%s", dayOfWeek, month, year), monthYear, "0"});
                }
            }
        }
        for (Commit commit : commitList) {
            ZonedDateTime mergeTime = commit.getMergeTime();
            String id = String.format("%s,%sS,%s,%s", mergeTime.format(DateTimeFormatter.ofPattern("hh a")),
                    mergeTime.getDayOfWeek(), mergeTime.getMonth(), mergeTime.getYear());
            countMap.putIfAbsent(id, 0);
            countMap.put(id, countMap.get(id) + 1);
        }

        for (Map.Entry<String, Integer> idCount : countMap.entrySet()) {
            String id = idCount.getKey();
            String parentId = id.substring(id.indexOf(",") + 1);
            dayTime.add(new String[]{id, parentId, "" + idCount.getValue()});
        }
        return toResponse(dayTime);
    }

    @GET
    @Path("/reverts")
    public Response commitsReverts() {
        List<Commit> commitList = dbHandler.commits();
        Map<String, Integer> commitSizeMap = new HashMap<>();
        for (Commit commit : commitList) {
            if (!commit.getMessage().toLowerCase().contains("revert")) {
                continue;
            }
            String id = String.format("%s", commit.getProject());
            commitSizeMap.putIfAbsent(id, 0);
            commitSizeMap.put(id, commitSizeMap.get(id) + 1);
        }
        return toResponse(new TreeMap<>(commitSizeMap));
    }

    @GET
    @Path("/developers")
    public Response commitsByDevelopers() {
        List<Commit> commitList = dbHandler.commits();
        Map<String, Long> projectCountMap = commitList.stream().collect(Collectors.groupingBy(
                commit -> commit.getAuthEmail().toLowerCase().replace("@apigee.com", ""), TreeMap::new, Collectors.counting()));
        return toResponse(new TreeMap<>(projectCountMap));
    }

    @GET
    @Path("/developer-day-of-week")
    public Response commitsByDeveloperDayOfWeek() {
        List<Commit> commitList = dbHandler.commits();
        Map<String, Long> authDayCountMap = commitList.stream().collect(Collectors.groupingBy(
                commit -> commit.getAuthEmail().toLowerCase().replace("@apigee.com", "") + "-" + commit.getAuthTime().getDayOfWeek(), Collectors.counting()));
        Map<String, String> devDayOfWeek = new HashMap<>();
        for (Map.Entry<String, Long> authCount : authDayCountMap.entrySet()) {
            String author = authCount.getKey().split("-")[0].trim();
            String day = authCount.getKey().split("-")[1].trim();
            Long count = authCount.getValue();
            String dayCountString = devDayOfWeek.get(author);
            if (dayCountString == null) {
                devDayOfWeek.put(author, day + "-" + count);
                continue;
            }
            Integer c = Integer.parseInt(dayCountString.split("-")[1]);
            if (c <= count) {
                devDayOfWeek.put(author, day + "-" + count);
            }
        }

        Map<DayOfWeek, String> dayOfWeek = new HashMap<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            dayOfWeek.put(day, "");
        }

        for (Map.Entry<String, String> devDay : devDayOfWeek.entrySet()) {
            String dev = devDay.getKey();
            DayOfWeek day = DayOfWeek.valueOf(devDay.getValue().split("-")[0].trim());
            String count = devDay.getValue().split("-")[1].trim();
            String devs = dayOfWeek.get(day);
            dayOfWeek.put(day, devs + String.format(", %s(%s)", dev, count));
        }

        for (Map.Entry<DayOfWeek, String> dayDevs : dayOfWeek.entrySet()) {
            String value = dayDevs.getValue();
            if (value.startsWith(",")) {
                dayOfWeek.put(dayDevs.getKey(), value.substring(1));
            }
        }

        return toResponse(new TreeMap<>(dayOfWeek));
    }

    @GET
    @Path("/reviewers")
    public Response commitsByReviewers() {
        List<Commit> commitList = dbHandler.commits();
        Map<String, Long> reviewerCountMap = commitList.stream().collect(Collectors.groupingBy(
                commit -> commit.getMergeEmail().toLowerCase().replace("@apigee.com", ""), TreeMap::new, Collectors.counting()));
        return toResponse(new TreeMap<>(reviewerCountMap));
    }

    @GET
    @Path("/date-wise-stats")
    public Response commitsDateWiseStats() {
        List<Commit> commitList = dbHandler.commits();
        Map<String, String> commitStats = new HashMap<>();
        commitStats.put("total", "" + commitList.size());
        commitStats.put("reverts", "" + commitList.stream().filter(c -> c.getMessage().toLowerCase().contains("revert")).count());
        commitStats.put("branch", dbHandler.getGitBranch());
        commitStats.put("dateRange", dbHandler.getDateRange());
        commitStats.put("team", LoaderProperties.getInstance().getProperty(TEAM_NAME_PROPERTY));
        return toResponse(commitStats);
    }


    @GET
    @Path("/release-sizes")
    public Response releaseSizes() {
        List<Commit> commitList = dbHandler.commits();
        Map<String, Integer> releaseSizes = new LinkedHashMap<>();
        int size = 0;
        for (Commit commit : commitList) {
            ++size;
            String tag = commit.getTag();
            if (tag != null) {
                tag = tag.substring(tag.lastIndexOf('/') + 1);
                releaseSizes.put(tag, size);
                size = 0;
            }
        }
        return toResponse(releaseSizes);
    }

    @GET
    @Path("/release-composition")
    public Response releaseComposition() {
        List<Commit> commitList = dbHandler.commits();
        Map<String, Object> releaseComposition = new LinkedHashMap<>();
        Set<String> projects = commitList.stream().map(c -> c.getProject()).collect(Collectors.toCollection(TreeSet::new));
        String headerRow = "header";
        releaseComposition.put(headerRow, projects);
        Map<String, Integer> projectWiseCount = new HashMap<>();
        initProjectMap(projects, projectWiseCount);
        for (Commit commit : commitList) {
            String project = commit.getProject();
            projectWiseCount.putIfAbsent(project, 0);
            projectWiseCount.put(project, projectWiseCount.get(project) + 1);
            String tag = commit.getTag();
            if (tag != null) {
                tag = tag.substring(tag.lastIndexOf('/') + 1);
                releaseComposition.put(tag, new TreeMap<>(projectWiseCount));
                initProjectMap(projects, projectWiseCount);
            }
        }
        for (String releases : releaseComposition.keySet()) {
            if (releases.startsWith(headerRow)) {
                continue;
            }
            projectWiseCount = (Map<String, Integer>) releaseComposition.get(releases);
            Object[] counts = new Object[projects.size()];
            int index = 0;
            for (Integer v : projectWiseCount.values()) {
                counts[index++] = v;
            }
            releaseComposition.put(releases, counts);
        }

        return toResponse(releaseComposition);
    }

    private void initProjectMap(Set<String> projects, Map<String, Integer> projectWiseCount) {
        projectWiseCount.clear();
        for (String project : projects) {
            projectWiseCount.put(project, 0);
        }
    }

}
