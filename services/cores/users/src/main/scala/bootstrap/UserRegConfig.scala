package ru.otus.sales.leads.generator.services.cores.users
package bootstrap

import repositories.UserRepository
import services.UserRegService
import services.UserRegService.UserRegService
import ru.otus.sales.leads.generator.inf.repository.transactors.DBTransactor
import ru.otus.sales.leads.generator.services.cores.users.validators.UserRegValidator
import zio.{ULayer, URLayer}

object UserRegConfig {
  val live: ULayer[UserRegService] =
    UserRepository.live ++ UserRegValidator.live >>> UserRegService.live
}
