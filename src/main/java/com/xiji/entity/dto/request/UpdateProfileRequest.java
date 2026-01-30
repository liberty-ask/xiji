package com.xiji.entity.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新用户资料请求DTO
 * @author liberty
 */
@Data
public class UpdateProfileRequest {
    
    @Size(max = 50, message = "昵称长度不能超过50个字符")
    private String nickname;
    
    private String avatar;
    //邮箱
    private String email;
}
