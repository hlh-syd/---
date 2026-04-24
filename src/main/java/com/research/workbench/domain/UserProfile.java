package com.research.workbench.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "user_profile")
public class UserProfile extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "real_name", length = 64)
    private String realName;

    private Integer gender;

    @Column(length = 500)
    private String bio;

    @Column(length = 128)
    private String institution;

    @Column(length = 128)
    private String department;

    @Column(name = "research_direction", length = 255)
    private String researchDirection;

    @Column(name = "degree_level", length = 32)
    private String degreeLevel;

    @Column(columnDefinition = "json")
    private String interests;

    @Column(columnDefinition = "json")
    private String tags;
}
