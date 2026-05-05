package com.skillbridge.gig.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "gigs", schema = "gigs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Gig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "freelancer_id", nullable = false)
    private Integer freelancerId;

    @Column(name = "category_id", insertable = false, updatable = false)
    private Integer categoryId;

    @Column(length = 255, nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal cost;

    @Column(name = "delivery_time", nullable = false)
    private Integer deliveryTime;

    @Column(name = "revision_count", nullable = false)
    private Integer revisionCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private GigStatus status = GigStatus.ACTIVE;

    @Column(name = "cover_image", length = 500)
    private String coverImage;

    @Column(name = "freelancer_name", length = 255)
    private String freelancerName;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToMany(fetch = FetchType.LAZY)
    @BatchSize(size = 20)
    @JoinTable(
        name = "gig_tags", schema = "gigs",
        joinColumns = @JoinColumn(name = "gig_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private List<Tag> tags = new ArrayList<>();

    @OneToMany(mappedBy = "gig", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 20)
    @OrderBy("sortOrder ASC")
    private List<GigImage> images = new ArrayList<>();
}
