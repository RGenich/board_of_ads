package com.board_of_ads.models.posting.forDogs;

import com.board_of_ads.models.Category;
import com.board_of_ads.models.User;
import com.board_of_ads.models.posting.Posting;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

@Data
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "posting_dogs")
public class dogsPosting extends Posting {
    @Column
    private String contactEmail;

    @Column
    private String linkYouTube;

    @Column
    private String communicationType;

    @Column
    private String breed;

    public dogsPosting (User user, Category category, String title, String description, Long price, String contact, Boolean isActive,
                        String contactEmail, String linkYouTube, String communicationType, String breed) {
        super(user, category, title, description, price, contact, isActive);
        this.contactEmail = contactEmail;
        this.linkYouTube = linkYouTube;
        this.communicationType = communicationType;
        this.breed = breed;
    }
}