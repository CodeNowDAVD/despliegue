package biblioteca.gorbits.security;

import java.util.Collection;
import java.util.List;
import biblioteca.gorbits.user.Role;
import biblioteca.gorbits.user.UserAccount;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class AccountPrincipal implements UserDetails {

    private final UserAccount account;

    public AccountPrincipal(UserAccount account) {
        this.account = account;
    }

    public UserAccount account() {
        return account;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Role r = account.getRole();
        return List.of(new SimpleGrantedAuthority("ROLE_" + r.name()));
    }

    @Override
    public String getPassword() {
        return account.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return account.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return account.isEnabled();
    }
}
