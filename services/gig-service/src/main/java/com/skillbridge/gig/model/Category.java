package com.skillbridge.gig.model;

import jakarta.persistence.*;

@Entity
@Table(name = "categories", schema = "gigs")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 255, nullable = false)
    private String title;

    @Column(length = 255, nullable = false, unique = true)
    private String slug;

    public Category() {}

    public Category(String title, String slug) {
        this.title = title;
        this.slug = slug;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
}
