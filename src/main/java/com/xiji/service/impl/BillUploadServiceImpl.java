package com.xiji.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiji.entity.domain.BillUpload;
import com.xiji.mapper.BillUploadMapper;
import com.xiji.service.BillUploadService;
import org.springframework.stereotype.Service;

/**
 * 账单上传服务实现类
 * @author liberty
 */
@Service
public class BillUploadServiceImpl extends ServiceImpl<BillUploadMapper, BillUpload> implements BillUploadService {
}


