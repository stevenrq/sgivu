package com.sgivu.auth.dto;

import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Permission {
  private Long id;
  private String name;
  private Set<Role> roles = new HashSet<>();
}
