package com.liuliu.example.myblogbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liuliu.example.myblogbackend.common.ErrorCode;
import com.liuliu.example.myblogbackend.dto.LoginRequest;
import com.liuliu.example.myblogbackend.dto.LoginResponse;
import com.liuliu.example.myblogbackend.dto.RegisterRequest;
import com.liuliu.example.myblogbackend.dto.ResetPasswordRequest;
import com.liuliu.example.myblogbackend.dto.UserVO;
import com.liuliu.example.myblogbackend.entity.User;
import com.liuliu.example.myblogbackend.exception.BusinessException;
import com.liuliu.example.myblogbackend.mapper.UserMapper;
import com.liuliu.example.myblogbackend.util.EmailUtil;
import com.liuliu.example.myblogbackend.util.JwtUtil;
import com.liuliu.example.myblogbackend.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private EmailUtil emailUtil;

    public UserVO register(RegisterRequest req){
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getEmail,req.getEmail())
        );
        if (count!=null && count>0){
            throw new BusinessException(ErrorCode.CONFLICT, "该邮箱已被注册");
        }

        User user = new User();
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setNickname(req.getNickname());

        userMapper.insert(user);

        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setEmail(user.getEmail());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        return vo;
    }

    @Autowired
    private JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest req){
        // 1. 查用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail,req.getEmail())
        );
        if (user==null){
            throw new BusinessException(ErrorCode.PARAM_ERROR, "邮箱或密码错误");
        }

        // 2. 校验密码
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "邮箱或密码错误");
        }

        // 3. 生成 token
        String token = jwtUtil.generateToken(user.getId(), user.getEmail());

        LoginResponse resp = new LoginResponse();
        resp.setToken(token);
        LoginResponse.UserInfo info = new LoginResponse.UserInfo();
        info.setId(user.getId());
        info.setNickname(user.getNickname());
        info.setEmail(user.getEmail()); 
        info.setAvatar(user.getAvatar());
        resp.setUser(info);
        return resp;
    }

    public LoginResponse.UserInfo getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user==null){
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        LoginResponse.UserInfo info = new LoginResponse.UserInfo();
        info.setId(user.getId());
        info.setEmail(user.getEmail());
        info.setNickname(user.getNickname());
        info.setAvatar(user.getAvatar());
        return info;
    }

    /** 更新头像 URL */
    public LoginResponse.UserInfo updateAvatar(Long userId, String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "头像地址不能为空");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        user.setAvatar(avatarUrl);
        userMapper.updateById(user);

        LoginResponse.UserInfo info = new LoginResponse.UserInfo();
        info.setId(user.getId());
        info.setEmail(user.getEmail());
        info.setNickname(user.getNickname());
        info.setAvatar(user.getAvatar());
        return info;
    }

    /** 发送重置密码验证码 */
    public void sendResetCode(String email) {
        // 1. 检查邮箱是否存在
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, email)
        );
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "该邮箱未注册");
        }

        // 2. 频率限制：60 秒内不能重复发
        String limitKey = "email:limit:" + email;
        if (Boolean.TRUE.equals(redisUtil.hasKey(limitKey))) {
            throw new BusinessException (ErrorCode.TOO_MANY_REQUESTS, "发送太频繁，请 60 秒后再试" );
        }

        // 次数限制：每分钟最多5次
        String rateKey = "rate:send-code:" + email;
        if (!redisUtil.tryAcquire(rateKey, 5, 60)) {
            throw new BusinessException (ErrorCode.TOO_MANY_REQUESTS, "操作太频繁，请稍后再试" );
        }

        // 3. 生成 6 位验证码
        String code = String.format("%06d", new Random().nextInt(1000000));

        // 4. 存库，10 分钟有效
        String codeKey = "email:code:" + email;
        redisUtil.set(codeKey, code, 600);

        // 5. 发邮件
        emailUtil.sendCode(email, code);

        // 6. 发送成功后写入冷却标记，60 秒内禁止重复发送
        redisUtil.set(limitKey, "1" , 60 );
    }

    /** 用验证码重置密码 */
    public void resetPassword(ResetPasswordRequest req) {
        String codeKey = "email:code:" + req.getEmail();
        String cachedCode = redisUtil.get(codeKey);

        if (cachedCode == null) {
            throw new BusinessException (ErrorCode.PARAM_ERROR, "验证码已过期，请重新获取" );
        }
        if (!cachedCode.equals(req.getCode())) {
            throw new BusinessException (ErrorCode.PARAM_ERROR, "验证码错误" );
        }

        // 更新密码
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, req.getEmail())
        );
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userMapper.updateById(user);

        // 删除验证码，防止重复使用
        redisUtil.delete(codeKey);
    }
}
