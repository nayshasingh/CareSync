package com.cts.healthcare_appointment_system.services;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.cts.healthcare_appointment_system.dto.ChangePasswordDTO;
import com.cts.healthcare_appointment_system.dto.JwtDTO;
import com.cts.healthcare_appointment_system.dto.UserDTO;
import com.cts.healthcare_appointment_system.dto.UserLoginDTO;
import com.cts.healthcare_appointment_system.dto.UserResponseDTO;
import com.cts.healthcare_appointment_system.dto.UserUpdateDTO;
import com.cts.healthcare_appointment_system.enums.AppointmentStatus;
import com.cts.healthcare_appointment_system.enums.UserRole;
import com.cts.healthcare_appointment_system.error.ApiException;
import com.cts.healthcare_appointment_system.models.User;
import com.cts.healthcare_appointment_system.repositories.UserRepository;
import com.cts.healthcare_appointment_system.security.JwtUtils;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@AllArgsConstructor
public class UserService {

    private final UserRepository userRepo;
    private PasswordEncoder passwordEncoder;
    private JwtUtils jwtUtils;
    private AuthenticationManager authManager;
    private AppointmentService appointmentService;
    private AuditLogService auditLogService;

    // GET methods
    // Get all users 
    public ResponseEntity<List<UserResponseDTO>> getAllusers() {
        List<User> users = userRepo.findAll();
        if (users.isEmpty()) {
            throw new ApiException("No users found", HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.status(HttpStatus.OK).body(users.stream().map(UserResponseDTO::from).toList());
    }

    //Get user by id
    public ResponseEntity<UserResponseDTO> getUserById(int id) {
        User user = userRepo.findById(id).orElse(null);
        if (user == null) {
            throw new ApiException("No user with user id: " + id + " found", HttpStatus.BAD_REQUEST);
        }
        return ResponseEntity.status(HttpStatus.OK).body(UserResponseDTO.from(user));
    }
    
    //Get user by email
    public ResponseEntity<UserResponseDTO> getUserByEmail(String email) {
        User user = userRepo.findByEmail(email).orElse(null);

        log.debug("Executing getUserByEmail() for: {}", email);

        if (user == null) {
            log.error("No user found in getUserByEmail() with email: {}", email);
            throw new ApiException("No user found with email: " + email + " found", HttpStatus.BAD_REQUEST);
        }
        return ResponseEntity.status(HttpStatus.OK).body(UserResponseDTO.from(user));
    }

    // PUT methods
    // Change user details
    @Transactional
    public ResponseEntity<UserResponseDTO> changeUserDetails(UserUpdateDTO dto){
        int userId = dto.getUserId();
        String name = dto.getName();
        String password = dto.getPassword();
        String phone = dto.getPhone();

        log.debug("Executing changeUserDetails() for userId: {}", userId);

        User user = userRepo.findById(userId).orElse(null);
        if(user == null){
            log.error("No user found in changeUserDetails() for userId: {}", userId);
            throw new ApiException("No user found with id: " + userId, HttpStatus.BAD_REQUEST);
        }
        user.setName(name);
        user.setPassword(passwordEncoder.encode(password));
        user.setPhone(phone);

        userRepo.save(user);
        auditLogService.record(user.getUserId(), "USER_PROFILE_UPDATED", "USER", user.getUserId(), "User profile details were updated");
        return ResponseEntity.status(HttpStatus.OK).body(UserResponseDTO.from(user));

    }

    // Change password of an user
    public ResponseEntity<UserResponseDTO> changeUserPassword(ChangePasswordDTO dto){
        String email = dto.getEmail();
        String newPassword = dto.getNewPassword();

        log.debug("Changing password for the user with email: {}", email);

        User user = userRepo.findByEmail(email).orElse(null);

        if (user == null) {
            throw new ApiException("No user found with email: " + email + " found", HttpStatus.BAD_REQUEST);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepo.save(user);
        auditLogService.record(user.getUserId(), "USER_PASSWORD_CHANGED", "USER", user.getUserId(), "User password was changed");

        return ResponseEntity.status(HttpStatus.OK).body(UserResponseDTO.from(user));
    }

    // POST methods
    // Register/save a new user
    @Transactional
    public ResponseEntity<UserResponseDTO> registerUser(UserDTO dto) {
        // userId is set to null so that JPA will think it's a new entity, will generate primary key value, and save it

        log.debug("Executing registerUser() for user with email: {}", dto.getEmail());

        // Check whether the email is already registered or not
        if(userRepo.findByEmail(dto.getEmail()).orElse(null) != null){
            log.error("Failed to register user with email: {}, as it is already registered", dto.getEmail());
            throw new ApiException("Email id already registered", HttpStatus.BAD_REQUEST);
        }

        if(dto.getRole() != UserRole.DOCTOR && dto.getRole() != UserRole.PATIENT){
            throw new ApiException("Invalid user role: " + dto.getRole(), HttpStatus.BAD_REQUEST);
        }

        User user = new User();        
        
        user.setUserId(null);
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setRole(dto.getRole());
        // Encode password before storing
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setPhone(dto.getPhone());

        User savedUser = userRepo.save(user);
        if (savedUser == null) {
            throw new ApiException("Failed to create new user", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        auditLogService.record(savedUser.getUserId(), "USER_REGISTERED", "USER", savedUser.getUserId(), "New user registered with role: " + savedUser.getRole());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponseDTO.from(savedUser));
    }

    // Login a user
    public ResponseEntity<JwtDTO> checkLogin(UserLoginDTO dto) {
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword()));

        JwtDTO jwtDto = new JwtDTO();
        User user = userRepo.findByEmail(dto.getEmail()).orElse(null);
        if (auth.isAuthenticated()) {

            log.debug("User with email: {} logged in", dto.getEmail());

            String jwt = jwtUtils.generateJWTToken(dto.getEmail());

            jwtDto.setEmail(dto.getEmail());
            jwtDto.setUserId(user.getUserId());
            jwtDto.setJwtToken(jwt);

            return ResponseEntity.status(HttpStatus.OK).body(jwtDto);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
    } 


    // DELETE methods
    // Remove an user
    @Transactional
    public ResponseEntity<UserResponseDTO> deleteUserById(int id) {
        User user = userRepo.findById(id).orElse(null);

        log.debug("Executing deleteUserById() for user with id: {}", id);

        if (user == null) {
            log.error("Failed to delete user as no user found with id: {}", id);
            throw new ApiException("No user with user id: " + id + " found", HttpStatus.BAD_REQUEST);
        }
        // Removing the associations
        user.getPatientAppointments().forEach(a -> {
            if(a.getStatus() == AppointmentStatus.BOOKED){
                appointmentService.cancelAppointment(a.getAppointmentId());
            }
            a.setPatient(null);
        });
        user.getDoctorAppointments().forEach(a -> {
            if(a.getStatus() == AppointmentStatus.BOOKED){
                appointmentService.cancelAppointment(a.getAppointmentId());
            }
            a.setDoctor(null);
        });
        // This will also remove the availabilities (Thanks to 'orphanRemoval = true')
        user.getAvailabilities().forEach(e -> e.setDoctor(null));

        UserResponseDTO response = UserResponseDTO.from(user);
        auditLogService.record(user.getUserId(), "USER_DELETED", "USER", user.getUserId(), "User account was deleted");
        userRepo.delete(user);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
