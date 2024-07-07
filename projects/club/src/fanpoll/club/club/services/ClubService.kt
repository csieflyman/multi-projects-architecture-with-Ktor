/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.club.club.services

import fanpoll.club.ClubUserRole
import fanpoll.club.club.domain.Club
import fanpoll.club.club.domain.ClubRole
import fanpoll.club.club.dtos.CreateClubForm
import fanpoll.club.club.dtos.UpdateClubForm
import fanpoll.club.club.repository.ClubRepository
import fanpoll.club.user.services.UserJoinClubService
import fanpoll.infra.base.exception.RequestException
import fanpoll.infra.base.response.InfraResponseCode

class ClubService(private val clubRepository: ClubRepository, private val userJoinClubService: UserJoinClubService) {

    suspend fun createClub(form: CreateClubForm) {
        return clubRepository.create(form.toEntity())
    }

    suspend fun updateClub(form: UpdateClubForm) {
        val joinedClub = userJoinClubService.getJoinedClub(form.currentUser.userId, form.id)
        if (joinedClub != null && (form.currentUser.userRoles.contains(ClubUserRole.Admin) || (joinedClub.role!! == ClubRole.Admin))) {
            clubRepository.update(form.toEntity())
        } else {
            throw RequestException(InfraResponseCode.AUTH_ROLE_FORBIDDEN, "${form.currentUser.account} is forbidden to update club")
        }
    }

    suspend fun getClubById(clubId: String): Club {
        return clubRepository.getById(clubId)
    }
}