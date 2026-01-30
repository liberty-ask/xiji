package com.xiji.config;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

/**
 * 验证码配置
 * @author liberty
 */
@Configuration
public class ImageConfig {
    @Bean
    public DefaultKaptcha getDefaultKaptcha() {
        DefaultKaptcha defaultKaptcha = new DefaultKaptcha();
        Properties properties = new Properties();
        //验证码是否有边框
        properties.setProperty("kaptcha.border", "yes");
        //边框颜色
        properties.setProperty("kaptcha.border.color", "105,179,90");
        //验证码字体颜色
        properties.setProperty("kaptcha.textproducer.font.color", "blue");
        //验证码图片宽度
        properties.setProperty("kaptcha_image_width", "150");
        //验证码图片高度
        properties.setProperty("kaptcha_image_height", "36");
        //生成验证码的字符
        properties.setProperty("kaptcha_textproducer_char_string", "0123456789");
        //去掉干扰线
        properties.setProperty("kaptcha_noise_impl", "com.ttl.config.imageCode.NoNoiseFactory");
        //字体大小
        properties.setProperty("kaptcha_textproducer_font_size", "36");
        //字体
        properties.setProperty("kaptcha_textproducer_font_names", "楷体");
        //验证码长度
        properties.setProperty("kaptcha_textproducer_char_length", "4");
        //图片效果
        properties.setProperty("kaptcha_obscurificator.impl", "com.google.code.kaptcha.impl.ShadeGimpy");
        Config config = new Config(properties);
        defaultKaptcha.setConfig(config);
        //返回
        return defaultKaptcha;
    }
}
