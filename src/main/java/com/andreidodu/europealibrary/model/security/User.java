package com.andreidodu.europealibrary.model.security;

import com.andreidodu.europealibrary.model.common.ModelCommon;
import com.andreidodu.europealibrary.model.stripe.StripeCustomer;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "el_user", uniqueConstraints = {@UniqueConstraint(columnNames = "username,email")})
@EntityListeners(AuditingEntityListener.class)
public class User extends ModelCommon {

    @Id
    @GeneratedValue(generator = "el_user_seq", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "el_user_seq", sequenceName = "el_user_seq", allocationSize = 50)
    private Long id;

    @Column(name = "username", unique = true)
    private String username;

    @Column(name = "email", unique = true)
    private String email;

    @NonNull
    @JsonIgnore
    private String password;

    @Column(name = "enabled")
    private Boolean enabled;

    @Column(name = "reset_password_token")
    private String resetToken;

    @Column(name = "reset_password_request_timestamp")
    private LocalDateTime recoveryRequestTimestamp;

    @OneToMany(mappedBy = "user", cascade = {CascadeType.PERSIST})
    private List<Authority> authorityList;

    @OneToOne(mappedBy = "user",fetch = FetchType.LAZY, optional = false)
    private StripeCustomer stripeCustomer;

    @OneToMany(mappedBy = "user")
    private List<Token> tokenList;

    @Column(name = "consensus1_flag")
    private Boolean consensus1Flag;

    @Column(name = "consensus2_flag")
    private Boolean consensus2Flag;

    @Column(name = "consensus3_flag")
    private Boolean consensus3Flag;

}
