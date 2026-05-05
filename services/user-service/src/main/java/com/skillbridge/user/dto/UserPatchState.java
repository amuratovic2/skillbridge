package com.skillbridge.user.dto;

import com.skillbridge.user.model.User;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserPatchState {

    @Size(max = 255)
    private String firstName;

    @Size(max = 255)
    private String lastName;

    @Size(max = 2000)
    private String bio;

    @Size(max = 500)
    private String profilePicture;

    @Size(max = 100)
    private String country;

    public static UserPatchState from(User user) {
        UserPatchState state = new UserPatchState();
        state.setFirstName(user.getFirstName());
        state.setLastName(user.getLastName());
        state.setBio(user.getBio());
        state.setProfilePicture(user.getProfilePicture());
        state.setCountry(user.getCountry());
        return state;
    }

    public void applyTo(User user) {
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setBio(bio);
        user.setProfilePicture(profilePicture);
        user.setCountry(country);
    }
}
