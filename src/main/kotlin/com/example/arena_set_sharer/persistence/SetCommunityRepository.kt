package com.example.arena_set_sharer.persistence

import com.example.arena_set_sharer.persistence.model.SetCommunityEntity
import org.springframework.data.jpa.repository.JpaRepository

interface SetCommunityRepository : JpaRepository<SetCommunityEntity, String>
