package com.tenderintelligence.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 项目的启动类
 *
 * 如果你碰到启动的问题，请认真阅读 https://doc.iocoder.cn/quick-start/ 文章
 * 如果你碰到启动的问题，请认真阅读 https://doc.iocoder.cn/quick-start/ 文章
 * 如果你碰到启动的问题，请认真阅读 https://doc.iocoder.cn/quick-start/ 文章
 *
 * @author 芋道源码
 */
@SuppressWarnings("SpringComponentScan") // 忽略 IDEA 无法识别 ${tender.info.base-package}
@SpringBootApplication(scanBasePackages = {"${tender.info.base-package}.server", "${tender.info.base-package}.module"})
public class TenderServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TenderServerApplication.class, args);
    }

}
