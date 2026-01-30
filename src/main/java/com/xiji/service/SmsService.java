package com.xiji.service;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teaopenapi.models.Config;
import com.xiji.config.CustomConfig;
import com.xiji.utils.RedisUtils;
import com.xiji.utils.ValidationUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Random;

/**
 * 短信服务类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsService {

    private final CustomConfig customConfig;
    private final RedisUtils redisUtils;
    
    /**
     * 短信发送结果
     */
    @Data
    @AllArgsConstructor
    public static class SmsSendResult {
        /**
         * 是否发送成功
         */
        private boolean success;
        
        /**
         * 验证码（仅在开发模式不发送短信时返回）
         */
        private String code;
    }
    
    /**
     * 创建阿里云短信客户端
     */
    private Client createClient() throws Exception {
        CustomConfig.SmsConfig smsConfig = customConfig.getSms();
        if (smsConfig == null || 
            smsConfig.getAccessKeyId() == null || 
            smsConfig.getAccessKeySecret() == null) {
            throw new RuntimeException("阿里云短信配置不完整");
        }
        
        Config config = new Config()
            .setAccessKeyId(smsConfig.getAccessKeyId())
            .setAccessKeySecret(smsConfig.getAccessKeySecret());
        config.endpoint = "dysmsapi.aliyuncs.com";
        
        return new Client(config);
    }
    
    /**
     * 生成6位数字验证码
     */
    private String generateCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }
    
    /**
     * 发送短信验证码
     * @param phone 手机号
     * @param type 类型：register-注册，login-登录，forgot-password-忘记密码
     * @return 发送结果，包含是否成功和验证码（开发模式）
     */
    public SmsSendResult sendSmsCode(String phone, String type) {
        try {
            // 验证手机号格式
            if (!ValidationUtils.isValidPhone(phone)) {
                log.warn("手机号格式不正确：{}", phone);
                return new SmsSendResult(false, null);
            }
            
            CustomConfig.SmsConfig smsConfig = customConfig.getSms();
            if (smsConfig == null) {
                log.error("短信配置不存在");
                return new SmsSendResult(false, null);
            }
            
            // 检查发送间隔
            String intervalKey = getIntervalKey(phone, type);
            if (redisUtils.hasKey(intervalKey)) {
                log.warn("发送过于频繁，手机号：{}", phone);
                return new SmsSendResult(false, null);
            }
            
            // 生成验证码
            String code = generateCode();
            
            // 判断是否启用短信发送
            boolean enable = smsConfig.getEnable() != null ? smsConfig.getEnable() : true;
            
            if (!enable) {
                // 开发模式：不发送短信，直接将验证码保存到Redis并返回
                String codeKey = getCodeKey(phone, type);
                Long expire = smsConfig.getCodeExpire() != null ? smsConfig.getCodeExpire() : 300L;
                redisUtils.set(codeKey, code, expire);
                
                // 设置发送间隔
                Long interval = smsConfig.getSendInterval() != null ? smsConfig.getSendInterval() : 60L;
                redisUtils.set(intervalKey, "1", interval);
                
                log.info("开发模式：验证码已生成（未发送短信），手机号：{}，类型：{}，验证码：{}", phone, type, code);
                return new SmsSendResult(true, code);
            } else {
                // 生产模式：发送短信
                Client client = createClient();
                SendSmsRequest request = new SendSmsRequest()
                    .setPhoneNumbers(phone)
                    .setSignName(smsConfig.getSignName())
                    .setTemplateCode(smsConfig.getTemplateCode())
                    .setTemplateParam("{\"code\":\"" + code + "\"}");
                
                SendSmsResponse response = client.sendSms(request);
                
                if ("OK".equals(response.getBody().getCode())) {
                    // 保存验证码到Redis
                    String codeKey = getCodeKey(phone, type);
                    Long expire = smsConfig.getCodeExpire() != null ? smsConfig.getCodeExpire() : 300L;
                    redisUtils.set(codeKey, code, expire);
                    
                    // 设置发送间隔
                    Long interval = smsConfig.getSendInterval() != null ? smsConfig.getSendInterval() : 60L;
                    redisUtils.set(intervalKey, "1", interval);
                    
                    log.info("短信验证码发送成功，手机号：{}，类型：{}", phone, type);
                    return new SmsSendResult(true, null);
                } else {
                    log.error("短信发送失败，手机号：{}，错误：{}", phone, response.getBody().getMessage());
                    return new SmsSendResult(false, null);
                }
            }
        } catch (Exception e) {
            log.error("发送短信验证码异常，手机号：{}", phone, e);
            return new SmsSendResult(false, null);
        }
    }
    
    /**
     * 验证短信验证码
     * @param phone 手机号
     * @param code 验证码
     * @param type 类型：register-注册，login-登录
     * @return 是否验证通过
     */
    public boolean verifySmsCode(String phone, String code, String type) {
        if (phone == null || code == null) {
            return false;
        }
        
        String codeKey = getCodeKey(phone, type);
        Object storedCodeObj = redisUtils.get(codeKey);
        if (storedCodeObj == null) {
            log.warn("短信验证码已过期或不存在，手机号：{}", phone);
            return false;
        }
        
        // 将存储的验证码转换为字符串进行比较（防止类型不匹配，如 Integer vs String）
        String storedCode = String.valueOf(storedCodeObj);
        boolean matches = storedCode.equals(code);
        
        if (matches) {
            // 验证成功后删除验证码（一次性使用）
            redisUtils.delete(codeKey);
            log.debug("短信验证码验证成功，手机号：{}", phone);
        } else {
            log.warn("短信验证码错误，手机号：{}，输入：{}，存储：{}（类型：{}）", 
                phone, code, storedCode, storedCodeObj.getClass().getSimpleName());
        }
        
        return matches;
    }
    
    /**
     * 获取验证码Redis key
     */
    private String getCodeKey(String phone, String type) {
        String prefix = customConfig.getRedis() != null 
            ? customConfig.getRedis().getKeyPrefix() 
            : "family:";
        return prefix + "sms:code:" + type + ":" + phone;
    }
    
    /**
     * 获取发送间隔Redis key
     */
    private String getIntervalKey(String phone, String type) {
        String prefix = customConfig.getRedis() != null 
            ? customConfig.getRedis().getKeyPrefix() 
            : "family:";
        return prefix + "sms:interval:" + type + ":" + phone;
    }
}
