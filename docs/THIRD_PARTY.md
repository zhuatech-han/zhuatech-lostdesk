# LostDesk 第三方与许可 / Third-party notices

知华科技（上海如静知华信息科技有限公司） · [官网](https://www.zhuatech.cn/) · 商业授权/定制微信 zhuatech、zhuatech2。

ZhiHua Technology (Shanghai Rujing Zhihua Information Technology Co., Ltd.) · [han@zhuatech.cn](mailto:han@zhuatech.cn), [jack@zhuatech.cn](mailto:jack@zhuatech.cn), [WhatsApp +86 17521234993](https://wa.me/8617521234993).

本项目自有代码遵循根目录非商业源码许可；第三方依赖和基础镜像保持其各自许可及版权，不因知华品牌变更许可。

Owned code uses the root non-commercial source license. Dependencies/images retain their own licenses and copyrights; ZhiHua branding does not replace their terms.

| Component | Use / upstream license |
| --- | --- |
| OpenJDK / Eclipse Temurin 21 | JVM / GPLv2 + Classpath exception |
| Spring Boot / Spring Security / Spring Data | web, auth, persistence / Apache-2.0 |
| Hibernate ORM | persistence / LGPL-2.1 |
| Flyway Community | migrations / Apache-2.0 |
| MySQL / Connector-J | database/driver / GPLv2 and upstream exceptions or applicable separate agreement |
| H2 / JUnit / Mockito | test dependencies; upstream notices apply |
| Vue / Vite | interface/build / MIT |
| Lucide | icons / ISC |
| Nginx | same-origin reverse proxy / BSD-style license |
| Requests / Black | local acceptance tools / Apache-2.0 / MIT |

依赖由Maven/npm/Docker拉取，不把第三方源码改署名为知华。部署或重新分发须检查对应版本的原始许可。当前没有已完成AI、快递、支付、身份系统或法规认证服务。

Dependencies are fetched from official registries; upstream code is not relabeled as ZhiHua. Check actual-version redistribution/deployment terms. No implemented AI/courier/payment/SSO/compliance service.
