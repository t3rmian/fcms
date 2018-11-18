package io.github.t3r1jj.fcms.external.authenticated.mega

import com.github.eliux.mega.MegaSession
import com.github.eliux.mega.MegaUtils
import com.github.eliux.mega.auth.MegaAuthCredentials
import com.github.eliux.mega.cmd.AbstractMegaCmdPathHandler
import com.github.eliux.mega.error.*
import io.github.t3r1jj.fcms.external.authenticated.AuthenticatedStorageTemplate
import io.github.t3r1jj.fcms.external.data.Record
import io.github.t3r1jj.fcms.external.data.RecordMeta
import io.github.t3r1jj.fcms.external.data.exception.StorageException
import io.github.t3r1jj.fcms.external.data.StorageInfo
import net.bytebuddy.ByteBuddy
import net.bytebuddy.agent.ByteBuddyAgent
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy
import net.bytebuddy.implementation.MethodDelegation
import net.bytebuddy.matcher.ElementMatchers
import java.io.FileInputStream
import java.nio.file.Paths

open class Mega(private val userName: String, private val password: String) : AuthenticatedStorageTemplate() {
    companion object {
        init {
            ByteBuddyAgent.install()
            ByteBuddy()
                    .redefine(MegaUtils::class.java)
                    .method(ElementMatchers.named("handleResult"))
                    .intercept(MethodDelegation.to(Mega::class.java))
                    .make()
                    .load(Mega::class.java.classLoader, ClassReloadingStrategy.fromInstalledAgent())
        }

        @Suppress("unused")
        @JvmStatic
        fun handleResult(code: Int) {
            val fixedCode = -code
            when (fixedCode) {
                0 -> {
                }
                -51 -> throw MegaWrongArgumentsException()
                -52 -> throw MegaInvalidEmailException()
                -53 -> throw MegaResourceNotFoundException()
                -54 -> throw MegaInvalidStateException()
                -55 -> throw MegaInvalidTypeException()
                -56 -> throw MegaOperationNotAllowedException()
                -57 -> throw MegaLoginRequiredException()
                -58 -> throw MegaNodesNotFetchedException()
                -59 -> throw MegaUnexpectedFailureException()
                -60 -> throw MegaConfirmationRequiredException()
                else -> throw MegaUnexpectedFailureException()
            }
        }
    }

    private var session: MegaSession? = null

    override fun login() {
        session = try {
            val currentSession = com.github.eliux.mega.Mega.currentSession()
            if (currentSession.whoAmI() != userName) {
                currentSession.logout()
                throw MegaException("Found another user session, logging out")
            }
            currentSession
        } catch (e: MegaException) {
            try {
                MegaAuthCredentials(userName, password).login()
            } catch (e: MegaException) {
                throw StorageException("Exception during login", e)
            }
        }
    }

    override fun isLogged(): Boolean {
        return session != null
    }

    override fun doAuthenticatedUpload(record: Record): RecordMeta {
        val file = stream2file(record.data)
        session!!.uploadFile(file.absolutePath, record.path)
                .createRemoteIfNotPresent<AbstractMegaCmdPathHandler>()
                .run()
        return RecordMeta(record.name, record.path, file.length())
    }

    override fun doAuthenticatedDownload(filePath: String): Record {
        val tempFile = java.io.File.createTempFile(System.currentTimeMillis().toString(), null)
        tempFile.delete()
        session!!.get(filePath)
                .setLocalPath(tempFile.absolutePath)
                .run()
        val path = Paths.get(filePath)
        return Record(path.fileName.toString(), filePath, FileInputStream(tempFile.absolutePath))
    }

    override fun doAuthenticatedFindAll(filePath: String): List<RecordMeta> {
        session!!
        return try {
            MegaCmdRecursiveList(filePath).recursiveCall()
        } catch (notFound: MegaResourceNotFoundException) {
            emptyList()
        }
    }

    override fun doAuthenticatedDelete(meta: RecordMeta) {
        session!!.remove(meta.path).run()
    }

    override fun isPresent(filePath: String): Boolean {
        return session!!.exists(filePath)
    }

    override fun doAuthenticatedGetInfo(): StorageInfo {
        session!!
        return MegaCmdDu().call()
    }

    override fun logout() {
        session?.logout()
        session = null
    }
}