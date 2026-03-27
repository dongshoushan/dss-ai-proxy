package com.dss.test.ai.proxy.task;

import com.dss.test.ai.proxy.service.ModelSwitchManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 模型重置定时任务
 * 每4小时强制重置为第一个模型
 *
 * @author dongshoushan
 * @工号 dwx1402878
 */
@Component
public class ModelResetScheduler {
    
    private static final Logger log = LoggerFactory.getLogger(ModelResetScheduler.class);
    
    private final ModelSwitchManager modelSwitchManager;
    
    /**
     * 构造函数
     *
     * @param modelSwitchManager 模型切换管理器
     */
    public ModelResetScheduler(ModelSwitchManager modelSwitchManager) {
        this.modelSwitchManager = modelSwitchManager;
    }
    
    /**
     * 每4小时重置模型为第一个
     * 使用cron表达式: 0 0 0/4 * * ?
     * - 0: 秒
     * - 0: 分
     * - 0/4: 每4小时执行一次(0点、4点、8点、12点、16点、20点)
     * - *: 每天
     * - *: 每月
     * - ?: 星期(不指定)
     *
     * @author dongshoushan
     * @工号 dwx1402878
     */
    @Scheduled(cron = "0 0 0/4 * * ?")
    public void resetModelEveryFourHours() {
        log.info("执行定时任务: 每4小时重置模型");
        modelSwitchManager.resetToFirstModel();
    }
}
