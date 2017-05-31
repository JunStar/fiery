package org.weiboad.ragnar.server.statistics.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.weiboad.ragnar.server.config.FieryConfig;
import org.weiboad.ragnar.server.struct.MetaLog;
import org.weiboad.ragnar.server.util.DateTimeHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Scope("singleton")
public class APIStatisticTimeSet {

    private ConcurrentHashMap<Long, APIStatisticURLSet> apiTopStaticHelper = new ConcurrentHashMap<Long, APIStatisticURLSet>();

    private Logger log = LoggerFactory.getLogger(APIStatisticTimeSet.class);

    @Autowired
    FieryConfig fieryConfig;

    public void analyzeMetaLog(MetaLog metainfo) {
        Long shardTime = metainfo.getTime().longValue();
        if (shardTime > 0) {
            shardTime = DateTimeHelper.getTimesMorning(shardTime);
            if (!apiTopStaticHelper.containsKey(shardTime)) {

                APIStatisticURLSet apiStatisticURLSet = new APIStatisticURLSet(shardTime);
                //count ++
                apiStatisticURLSet.analyzeMetaLog(metainfo);
                apiTopStaticHelper.put(shardTime, apiStatisticURLSet);
            } else {
                //count ++
                apiTopStaticHelper.get(shardTime).analyzeMetaLog(metainfo);
            }
        }
    }

    public Map<String, Integer> getAPITOPStatics() {
        Map<String, Integer> result = new LinkedHashMap<>();

        for (Map.Entry<Long, APIStatisticURLSet> ent : apiTopStaticHelper.entrySet()) {
            result.put(ent.getKey() + "", ent.getValue().getUrlSize());
        }
        return result;
    }

    public APIStatisticURLSet getSharder(Long timestamp, boolean create) {
        Long shardTime = DateTimeHelper.getTimesMorning(timestamp);
        if (!apiTopStaticHelper.containsKey(shardTime)) {
            if (create) {
                APIStatisticURLSet apiStatisticURLSet = new APIStatisticURLSet(shardTime);
                apiTopStaticHelper.put(shardTime, apiStatisticURLSet);
                return apiTopStaticHelper.get(shardTime);
            }
            //default not create this one
            return null;
        } else {
            return apiTopStaticHelper.get(shardTime);
        }
    }

    @Scheduled(fixedRate = 30 * 1000)
    public void cleanUpSharder() {
        if (apiTopStaticHelper.size() > 0) {
            ArrayList<Long> removeMap = new ArrayList<>();

            for (Map.Entry<Long, APIStatisticURLSet> ent : apiTopStaticHelper.entrySet()) {
                if (ent.getKey() >= DateTimeHelper.getCurrentTime() - fieryConfig.getKeepdataday() * 86400) {
                    continue;
                }
                removeMap.add(ent.getKey());
            }

            for (Long removeKey : removeMap) {
                log.info("Clean up the API Statistic:" + removeKey);
                apiTopStaticHelper.remove(removeKey);
            }
        }
    }
}
