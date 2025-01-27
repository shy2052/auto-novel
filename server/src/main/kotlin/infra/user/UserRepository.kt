package infra.user

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import infra.DataSourceMongo
import infra.DataSourceRedis
import infra.model.Page
import infra.model.User
import infra.model.UserFavored
import io.github.crackthecodeabhi.kreds.args.SetOption
import kotlinx.datetime.Clock
import org.bson.types.ObjectId
import org.litote.kmongo.*
import util.PBKDF2
import util.serialName
import kotlin.time.Duration.Companion.minutes

class UserRepository(
    private val mongo: DataSourceMongo,
    private val redis: DataSourceRedis,
) {
    suspend fun listUser(
        page: Int,
        pageSize: Int,
        role: User.Role,
    ): Page<User> {
        val users = mongo
            .userCollection
            .find(Filters.eq(User::role.path(), role.serialName()))
            .skip(page * pageSize)
            .limit(pageSize)
            .toList()

        val total = mongo
            .userCollection
            .countDocuments()

        return Page(
            items = users,
            total = total,
            pageSize = pageSize,
        )
    }

    suspend fun add(
        email: String,
        username: String,
        password: String,
    ): ObjectId {
        val salt = PBKDF2.randomSalt()
        val hashedPassword = PBKDF2.hash(password, salt)
        return mongo
            .userCollection
            .insertOne(
                User(
                    id = ObjectId(),
                    email = email,
                    username = username,
                    salt = salt,
                    password = hashedPassword,
                    role = User.Role.Normal,
                    favoredWeb = listOf(UserFavored(id = "default", title = "默认收藏夹")),
                    favoredWenku = listOf(UserFavored(id = "default", title = "默认收藏夹")),
                    createdAt = Clock.System.now(),
                )
            ).insertedId!!.asObjectId().value
    }

    suspend fun updatePassword(userId: ObjectId, password: String) {
        val salt = PBKDF2.randomSalt()
        val hashedPassword = PBKDF2.hash(password, salt)
        mongo
            .userCollection
            .updateOne(
                User::id eq userId,
                combine(
                    setValue(User::salt, salt),
                    setValue(User::password, hashedPassword),
                )
            )
    }

    suspend fun updateRole(userId: ObjectId, role: User.Role) {
        mongo
            .userCollection
            .updateOne(
                User::id eq userId,
                Updates.set(User::role.path(), role.serialName()),
            )
    }

    suspend fun getById(id: String): User? {
        return mongo
            .userCollection
            .findOne(User::id eq ObjectId(id))
    }

    suspend fun getByEmail(email: String): User? {
        return mongo
            .userCollection
            .findOne(User::email eq email)
    }

    suspend fun getByUsername(username: String): User? {
        return mongo
            .userCollection
            .findOne(User.byUsername(username))
    }

    suspend fun getByUsernameOrEmail(emailOrUsername: String): User? {
        return mongo
            .userCollection
            .findOne(
                or(
                    User::email eq emailOrUsername,
                    User::username eq emailOrUsername,
                )
            )
    }

    private fun emailCodeRedisKey(email: String) = "ec:${email}"

    suspend fun validateEmailCode(email: String, emailCode: String): Boolean {
        return redis.get(emailCodeRedisKey(email)) == emailCode
    }

    suspend fun addEmailCode(email: String, emailCode: String) {
        redis.set(
            key = emailCodeRedisKey(email),
            value = emailCode,
            setOption = SetOption.Builder()
                .exSeconds(15.minutes.inWholeSeconds.toULong())
                .build(),
        )
    }

    private fun resetPasswordTokenRedisKey(id: ObjectId) = "rpt:${id.toHexString()}"

    suspend fun validateResetPasswordToken(id: ObjectId, token: String): Boolean {
        return redis.get(resetPasswordTokenRedisKey(id)) == token
    }

    suspend fun addResetPasswordToken(id: ObjectId, token: String) {
        redis.set(
            key = resetPasswordTokenRedisKey(id),
            value = token,
            setOption = SetOption.Builder()
                .exSeconds(15.minutes.inWholeSeconds.toULong())
                .build(),
        )
    }
}