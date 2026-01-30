package com.xiji.entity.dto.response;

import lombok.Data;

/**
 * 天气响应数据
 */
@Data
public class Weather {
    private String city;
    private String maxTemp;
    private String minTemp;
    private String data;
}

