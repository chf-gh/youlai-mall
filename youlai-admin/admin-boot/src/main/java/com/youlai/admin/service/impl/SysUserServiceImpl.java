package com.youlai.admin.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.admin.pojo.dto.UserAuthDTO;
import com.youlai.admin.pojo.entity.SysUser;
import com.youlai.admin.mapper.SysUserMapper;
import com.youlai.admin.pojo.entity.SysUserRole;
import com.youlai.admin.service.ISysUserRoleService;
import com.youlai.admin.service.ISysUserService;
import com.youlai.common.constant.GlobalConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户业务类
 */
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService {

    private final PasswordEncoder passwordEncoder;
    private final ISysUserRoleService iSysUserRoleService;

    /**
     * 用户分页列表
     *
     * @param page
     * @param user
     * @return
     */
    @Override
    public IPage<SysUser> list(Page<SysUser> page, SysUser user) {
        List<SysUser> list = this.baseMapper.list(page, user);
        page.setRecords(list);
        return page;
    }

    /**
     * 新增用户
     *
     * @param user
     * @return
     */
    @Override
    public boolean saveUser(SysUser user) {
        user.setPassword(passwordEncoder.encode(GlobalConstants.DEFAULT_USER_PASSWORD));
        boolean result = this.save(user);
        if (result) {
            List<Long> roleIds = user.getRoleIds();
            if (CollectionUtil.isNotEmpty(roleIds)) {
                List<SysUserRole> userRoleList = new ArrayList<>();
                roleIds.forEach(roleId -> userRoleList.add(new SysUserRole().setUserId(user.getId()).setRoleId(roleId)));
                result = iSysUserRoleService.saveBatch(userRoleList);
            }
        }
        return result;
    }

    /**
     * 更新用户
     *
     * @param user
     * @return
     */
    @Override
    public boolean updateUser(SysUser user) {

        // 原来的用户角色ID集合
        List<Long> oldRoleIds = iSysUserRoleService.list(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, user.getId())).stream()
                .map(item -> item.getRoleId())
                .collect(Collectors.toList());

        // 新的用户角色ID集合
        List<Long> newRoleIds = user.getRoleIds();

        // 需要新增的用户角色ID集合
        List<Long> addRoleIds = newRoleIds.stream().filter(roleId -> !oldRoleIds.contains(roleId)).collect(Collectors.toList());
        if (CollectionUtil.isNotEmpty(addRoleIds)) {
            List<SysUserRole> addUserRoleList = new ArrayList<>();
            addRoleIds.forEach(roleId -> {
                addUserRoleList.add(new SysUserRole().setUserId(user.getId()).setRoleId(roleId));
            });
            iSysUserRoleService.saveBatch(addUserRoleList);
        }

        // 需要删除的用户的角色ID集合
        List<Long> removeRoleIds = oldRoleIds.stream().filter(roleId -> !newRoleIds.contains(roleId)).collect(Collectors.toList());
        if (CollectionUtil.isNotEmpty(removeRoleIds)) {
            removeRoleIds.forEach(roleId -> {
                iSysUserRoleService.remove(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, user.getId()).eq(SysUserRole::getRoleId, roleId));
            });
        }

        // 最后更新用户
        return this.updateById(user);
    }

    @Override
    public UserAuthDTO getByUsername(String username) {
        UserAuthDTO userAuthInfo = this.baseMapper.getByUsername(username);
        return userAuthInfo;
    }

}
