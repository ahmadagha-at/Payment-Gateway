package at.ahmad.paymentgateway.service;


import at.ahmad.paymentgateway.dtos.CreateUserDto;
import at.ahmad.paymentgateway.model.User;
import at.ahmad.paymentgateway.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepo;

    public UserService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    public User createUser(CreateUserDto dto) {
        User user = new User();
        user.setName(dto.name());
        user.setEmail(dto.email());
        return userRepo.save(user);
    }

    public List<User> getAllUsers() {
        return userRepo.findAll();
    }

    public User getUserById(Long id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User with ID " + id + " not found."));
    }

    public void deleteUser(Long id) {
        User user = getUserById(id);
        userRepo.delete(user);
    }
}
