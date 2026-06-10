package com.backend.amc_portal.auth.service;

import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

  private final JavaMailSender mailSender;

  @Value("${spring.mail.username}")
  private String from;

  public void sendVerificationCode(String to, String code) {
    String subject = "[AMC Portal] 이메일 인증 코드";
    String html =
        """
                <div style="font-family: sans-serif; max-width:600px; padding:24px;">
                  <h2>AMC Portal 이메일 인증</h2>
                  <p>아래 6자리 코드를 입력해주세요. 10분 후 만료됩니다.</p>
                  <p style="font-size:28px; font-weight:bold; letter-spacing:6px; padding:16px; background:#f3f4f6; text-align:center; border-radius:8px;">
                    %s
                  </p>
                  <p style="color:#6b7280; font-size:13px;">본 메일은 자동 발송되었습니다.</p>
                </div>
                """
            .formatted(code);
    sendHtml(to, subject, html);
  }

  public void sendPasswordResetLink(String to, String link) {
    String subject = "[AMC Portal] 비밀번호 재설정 안내";
    String html =
        """
                <div style="font-family: sans-serif; max-width:600px; padding:24px;">
                  <h2>비밀번호 재설정</h2>
                  <p>아래 버튼을 눌러 새 비밀번호를 설정해주세요. 30분 후 만료됩니다.</p>
                  <p style="text-align:center; margin: 24px 0;">
                    <a href="%s" style="background:#2563eb; color:white; padding:12px 24px; border-radius:6px; text-decoration:none;">
                      비밀번호 재설정
                    </a>
                  </p>
                  <p style="color:#6b7280; font-size:13px;">본인이 요청하지 않았다면 이 메일을 무시해주세요.</p>
                </div>
                """
            .formatted(link);
    sendHtml(to, subject, html);
  }

  private void sendHtml(String to, String subject, String html) {
    try {
      MimeMessage msg = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(msg, false, StandardCharsets.UTF_8.name());
      helper.setFrom(from);
      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(html, true);
      mailSender.send(msg);
      log.info("Email sent to {} ({})", to, subject);
    } catch (Exception e) {
      log.error("Failed to send email to {}: {}", to, e.getMessage());
      throw new RuntimeException("이메일 발송 실패: " + e.getMessage(), e);
    }
  }
}
