package com.sgivu.auth.dto;

import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Role {
  private Long id;
  private String name;
  private Set<Permission> permissions = new HashSet<>();
}
