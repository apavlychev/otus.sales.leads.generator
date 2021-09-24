package ru.otus.sales.leads.generator.apps.api
package api

import sttp.tapir.ztapir.{ZEndpoint, ZServerEndpoint}
import ru.otus.sales.leads.generator.inf.common.extensions.ListOpts
import ru.otus.sales.leads.generator.inf.repository.transactors.DBTransactor
import ru.otus.sales.leads.generator.services.cores.leads.models.{LeadError, LeadInfo}
import ru.otus.sales.leads.generator.services.cores.leads.services.LeadService.LeadService
import ru.otus.sales.leads.generator.services.cores.users.services.UserLoginService.{
  UserLoginService,
  login
}
import sttp.tapir.endpoint
import sttp.tapir.json.circe.jsonBody
import zio.logging.{LogAnnotation, Logging, log}
import sttp.tapir.generic.auto._
import sttp.tapir.server.http4s.ztapir.ZHttp4sServerInterpreter
import zio.clock.Clock
import sttp.tapir.ztapir._
import io.circe.generic.auto._
import org.http4s.HttpRoutes
import ru.otus.sales.leads.generator.apps.api.logging.Logger.botId
import ru.otus.sales.leads.generator.data.domain.entities.BotId
import ru.otus.sales.leads.generator.services.cores.leads.services.LeadService
import ru.otus.sales.leads.generator.services.cores.users.models.UserLogin
import zio.{IO, RIO, UIO, ZIO}
//import sttp.tapir._

import java.util.UUID

class LeadApi[R <: UserLoginService with LeadService with DBTransactor with Logging] {

  val createEndpoint: ZEndpoint[(Int, LeadInfo), ::[LeadError], Boolean] =
    endpoint
      .description("Создание лида")
      .post
      .in("leads" / "create")
      .in(header[Int]("X-Auth-Token"))
      //.in(auth.apiKey())
      .in(jsonBody[LeadInfo]
        .description("Модель лида")
        .example(LeadInfo("Павлычев Александр", "89202921268", 156.1)))
      .errorOut(jsonBody[::[LeadError]]
        .description("Ошибки создания лида")
        .example(~LeadError.BadPrice(BigDecimal(17.4))))
      .out(plainBody[Boolean])

  val createServerEndpoint: ZServerEndpoint[R, (Int, LeadInfo), ::[LeadError], Boolean] =
    createEndpoint.zServerLogic { case (bot, info) =>
      for {
        correlationId <- UIO(Some(UUID.randomUUID()))
        user <- login(UserLogin(bot)).orElseFail(~LeadError.LostConnection)
        _ <- log.locally(
          _.annotate(botId, bot).annotate(LogAnnotation.CorrelationId, correlationId)) {
          LeadService.create(info, user)
        }
      } yield true
    }

  val createRoutes: HttpRoutes[ZIO[R with Clock, Throwable, *]] =
    ZHttp4sServerInterpreter[R]()
      .from(createServerEndpoint)
      .toRoutes
}