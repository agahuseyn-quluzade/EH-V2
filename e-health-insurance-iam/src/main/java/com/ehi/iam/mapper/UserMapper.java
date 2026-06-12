package com.ehi.iam.mapper;

import com.ehi.iam.dto.response.UserDto;
import com.ehi.iam.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDto toDto(User user);
}
