package cn.wildfirechat.app.shiro;

import cn.wildfirechat.app.jpa.ShiroSession;
import cn.wildfirechat.app.jpa.ShiroSessionRepository;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.session.mgt.eis.SessionDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class DBSessionDao implements SessionDAO {
    private static final Logger LOG = LoggerFactory.getLogger(DBSessionDao.class);

    /**
     * Session data is deserialized from the database. Restrict the classes that may appear in the
     * stream so a compromised/corrupted session row cannot be used for a Java deserialization
     * gadget chain.
     */
    private static final Set<String> ALLOWED_SESSION_CLASSES = new HashSet<>(Arrays.asList(
        "org.apache.shiro.session.mgt.SimpleSession",
        "org.apache.shiro.subject.SimplePrincipalCollection",
        "java.lang.String",
        "java.lang.Long",
        "java.lang.Integer",
        "java.lang.Boolean",
        "java.lang.Number",
        "java.util.Date",
        "java.util.HashMap",
        "java.util.LinkedHashMap",
        "java.util.HashSet",
        "java.util.LinkedHashSet",
        "java.util.ArrayList",
        "java.util.Collections$EmptyMap",
        "java.util.Collections$EmptySet",
        "java.util.Collections$UnmodifiableMap",
        "java.util.Collections$UnmodifiableSet"
    ));

    @Autowired
    private ShiroSessionRepository shiroSessionRepository;

    @Override
    public Serializable create(Session session) {
        String sessionId = newSessionId();
        ((SimpleSession) session).setId(sessionId);
        return sessionId;
    }

    @Override
    public Session readSession(Serializable sessionId) throws UnknownSessionException {
        ShiroSession shiroSession = shiroSessionRepository.findById((String) sessionId).orElse(null);
        if (shiroSession != null) {
            return byteToSession(shiroSession.getSessionData());
        }
        return null;
    }

    @Override
    public void update(Session session) throws UnknownSessionException {
        byte[] data = sessionToByte(session);
        if (data == null) {
            LOG.warn("Unable to serialize session {}", session.getId());
            return;
        }
        Object userIdAttribute = session.getAttribute("userId");
        String userId = userIdAttribute == null ? null : userIdAttribute.toString();
        shiroSessionRepository.save(new ShiroSession((String) session.getId(), userId, data));
    }

    @Override
    public void delete(Session session) {
        if (session != null && session.getId() != null) {
            shiroSessionRepository.deleteById((String) session.getId());
        }
    }

    public void deleteByUserId(String userId) {
        if (userId != null) {
            shiroSessionRepository.deleteByUserId(userId);
        }
    }

    /**
     * Rotate the session id after a successful login to prevent session fixation.
     */
    public void rotate(Session session) {
        if (!(session instanceof SimpleSession)) {
            return;
        }
        String oldId = String.valueOf(session.getId());
        String newId = newSessionId();
        ((SimpleSession) session).setId(newId);
        shiroSessionRepository.deleteById(oldId);
        update(session);
    }

    @Override
    public Collection<Session> getActiveSessions() {
        List<Session> sessions = new ArrayList<>();
        for (ShiroSession shiroSession : shiroSessionRepository.findAll()) {
            Session session = byteToSession(shiroSession.getSessionData());
            if (session != null) {
                sessions.add(session);
            }
        }
        return sessions;
    }

    private String newSessionId() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    private byte[] sessionToByte(Session session) {
        try (ByteArrayOutputStream bo = new ByteArrayOutputStream();
             ObjectOutputStream oo = new ObjectOutputStream(bo)) {
            oo.writeObject(session);
            return bo.toByteArray();
        } catch (IOException e) {
            LOG.error("Serialize session error", e);
            return null;
        }
    }

    private Session byteToSession(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try (InputStream bi = new ByteArrayInputStream(bytes);
             ObjectInputStream in = new SafeObjectInputStream(bi)) {
            return (SimpleSession) in.readObject();
        } catch (ClassNotFoundException | IOException e) {
            LOG.warn("Deserialize session error: {}", e.getMessage());
            return null;
        }
    }

    private static final class SafeObjectInputStream extends ObjectInputStream {
        private SafeObjectInputStream(InputStream in) throws IOException {
            super(in);
        }

        @Override
        protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
            if (!ALLOWED_SESSION_CLASSES.contains(desc.getName())) {
                throw new InvalidClassException("Disallowed class in session data", desc.getName());
            }
            return super.resolveClass(desc);
        }
    }
}
