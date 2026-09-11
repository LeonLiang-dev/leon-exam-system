ALTER TABLE alone_auth_user
    MODIFY COLUMN PASSWORD varchar(128) NOT NULL COMMENT '密码(MD5旧密码或BCrypt新密码)';
