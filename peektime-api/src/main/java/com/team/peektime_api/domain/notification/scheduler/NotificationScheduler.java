package com.team.peektime_api.domain.notification.scheduler;

import com.team.peektime_api.domain.notification.service.FcmService;
import com.team.peektime_api.domain.solarterm.entity.SolarTerm;
import com.team.peektime_api.domain.solarterm.repository.SolarTermRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final FcmService fcmService;
    private final SolarTermRepository solarTermRepository;

    /**
     * 오늘의 미션 알림
     * 매일 오전 10시 발송
     */
    @Scheduled(cron = "0 0 10 * * *", zone = "Asia/Seoul")
    public void sendDailyMissionNotification() {
        log.info("오늘의 미션 알림 스케줄러 시작");
        try {
            solarTermRepository.findByDate(LocalDate.now())
                    .ifPresentOrElse(
                            this::sendDailyMission,
                            () -> log.warn("현재 절기 정보가 없어 오늘의 미션 알림을 건너뜁니다.")
                    );
        } catch (Exception e) {
            log.error("오늘의 미션 알림 전송 실패", e);
        }
    }

    private void sendDailyMission(SolarTerm solarTerm) {
        String title = solarTerm.getName() + "의 오늘의 미션이 도착했어요!";
        String body = "지금 바로 확인하고 제철을 즐겨보세요 🌿";

        fcmService.sendDailyMissionNotification(title, body);
        log.info("오늘의 미션 알림 전송 완료: 절기={}", solarTerm.getName());
    }

    /**
     * 절기 시작 알림
     * 절기 시작일 오전 8시 발송
     */
    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Seoul")
    public void sendSolarTermStartNotification() {
        log.info("절기 시작 알림 스케줄러 시작");
        try {
            LocalDate today = LocalDate.now();
            solarTermRepository.findByStartDate(today)
                    .ifPresentOrElse(
                            this::sendSolarTermStart,
                            () -> log.debug("오늘 시작하는 절기가 없습니다: {}", today)
                    );
        } catch (Exception e) {
            log.error("절기 시작 알림 전송 실패", e);
        }
    }

    private void sendSolarTermStart(SolarTerm solarTerm) {
        String title = "새로운 절기, " + solarTerm.getName() + "이 시작되었어요!";
        String body = "이번 절기에는 어떤 이야기가 담겨있을까요? 🌿";

        fcmService.sendSolarTermStartNotification(title, body, solarTerm.getName());
        log.info("절기 시작 알림 전송 완료: 절기={}", solarTerm.getName());
    }

    /**
     * 절기 종료 알림
     * 절기 마지막 날 오후 8시 발송
     */
    @Scheduled(cron = "0 0 20 * * *", zone = "Asia/Seoul")
    public void sendSolarTermEndNotification() {
        log.info("절기 종료 알림 스케줄러 시작");
        try {
            LocalDate today = LocalDate.now();
            solarTermRepository.findByEndDate(today)
                    .ifPresentOrElse(
                            this::sendSolarTermEnd,
                            () -> log.debug("오늘 종료하는 절기가 없습니다: {}", today)
                    );
        } catch (Exception e) {
            log.error("절기 종료 알림 전송 실패", e);
        }
    }

    private void sendSolarTermEnd(SolarTerm solarTerm) {
        String title = solarTerm.getName() + "의 마지막 날이에요";
        String body = "이번 절기에 완료하지 못한 미션이 있다면 지금 확인해보세요! 🍂";

        fcmService.sendSolarTermEndNotification(title, body, solarTerm.getName());
        log.info("절기 종료 알림 전송 완료: 절기={}", solarTerm.getName());
    }
}