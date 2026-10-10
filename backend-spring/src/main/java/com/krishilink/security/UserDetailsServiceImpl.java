package com.krishilink.security;

import com.krishilink.entity.User;
import com.krishilink.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String cleanEmail = email.trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(cleanEmail);

        if (userOpt.isEmpty()) {
            if (cleanEmail.endsWith("@krishilink.com")) {
                userOpt = userRepository.findByEmailIgnoreCase(cleanEmail.replace("@krishilink.com", "@agrilink.com"));
            } else if (cleanEmail.endsWith("@agrilink.com")) {
                userOpt = userRepository.findByEmailIgnoreCase(cleanEmail.replace("@agrilink.com", "@krishilink.com"));
            }
        }

        User user = userOpt.orElseThrow(() ->
                new UsernameNotFoundException("User not found with email: " + email));

        return new UserPrincipal(user);
    }

    public UserDetails loadUserById(Long id) {
        User user = userRepository.findById(id).orElseThrow(() ->
                new UsernameNotFoundException("User not found with id: " + id));
        return new UserPrincipal(user);
    }
}
