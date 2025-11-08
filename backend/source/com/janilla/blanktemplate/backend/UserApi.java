/*
 * MIT License
 *
 * Copyright (c) 2024-2025 Diego Schivo
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.janilla.blanktemplate.backend;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.janilla.cms.CollectionApi;
import com.janilla.http.HttpExchange;
import com.janilla.json.Jwt;
import com.janilla.persistence.Persistence;
import com.janilla.reflect.Reflection;
import com.janilla.web.BadRequestException;
import com.janilla.web.Bind;
import com.janilla.web.ForbiddenException;
import com.janilla.web.Handle;
import com.janilla.web.UnauthorizedException;

@Handle(path = "/api/users")
public class UserApi extends CollectionApi<Long, User> {

	public static final AtomicReference<UserApi> INSTANCE = new AtomicReference<>();

	protected final Properties configuration;

	public UserApi(Predicate<HttpExchange> drafts, Persistence persistence, Properties configuration) {
		super(User.class, drafts, persistence);
		this.configuration = configuration;
		if (!INSTANCE.compareAndSet(null, this))
			throw new IllegalStateException();
	}

	@Handle(method = "PUT", path = "(\\d+)")
	public User update(long id, User entity, Boolean draft, Boolean autosave, String password) {
		return super.update(id, entity.withPassword(password), draft, autosave);
	}

	@Handle(method = "POST", path = "login")
	public User login(User user, String password, CustomHttpExchange exchange) {
		if (user == null || user.email() == null || user.email().isBlank() || password == null || password.isBlank())
			throw new BadRequestException("Please correct invalid fields.");
		var u = crud().read(crud().find("email", user.email()));
		if (u != null && !u.passwordEquals(password))
			u = null;
		if (u == null)
			throw new UnauthorizedException("The email or password provided is incorrect.");
		var h = Map.of("alg", "HS256", "typ", "JWT");
		var p = Map.of("loggedInAs", u.email());
		var t = Jwt.generateToken(h, p, configuration.getProperty("blank-template.jwt.key"));
		exchange.setSessionCookie(t);
		return u;
	}

	@Handle(method = "POST", path = "logout")
	public void logout(CustomHttpExchange exchange) {
		exchange.setSessionCookie(null);
	}

	@Handle(method = "GET", path = "me")
	public User me(CustomHttpExchange exchange) {
		return exchange.sessionUser();
	}

	@Handle(method = "POST", path = "first-register")
	public User firstRegister(User user, String password, String confirmPassword, CustomHttpExchange exchange) {
		if (user == null || user.email() == null || user.email().isBlank() || password == null || password.isBlank()
				|| !password.equals(confirmPassword))
			throw new BadRequestException("Please correct invalid fields.");
		if (crud().count() != 0)
			throw new ForbiddenException("You are not allowed to perform this action.");
		var u = crud().create(user.withPassword(password));
		var h = Map.of("alg", "HS256", "typ", "JWT");
		var p = Map.of("loggedInAs", u.email());
		var t = Jwt.generateToken(h, p, configuration.getProperty("blank-template.jwt.key"));
		exchange.setSessionCookie(t);
		return u;
	}

	@Handle(method = "POST", path = "forgot-password")
	public void forgotPassword(User user) {
		if (user == null || user.email() == null || user.email().isBlank())
			throw new BadRequestException("Please correct invalid fields.");
		var u = crud().read(crud().find("email", user.email()));
		if (u == null)
			return;
		var t = UUID.randomUUID().toString().replace("-", "");
		u = crud().update(u.id(), x -> x.withResetPassword(t, Instant.now().plus(1, ChronoUnit.HOURS)));
		var h = "https://localhost:8443/admin/reset/" + t;
		var hm = """
				<p>
				  Hello Alice.<br>
				  You are receiving this because you (or someone else) have requested the reset of the password for your account.
				  Please click on the following link, or paste this into your browser to complete the process:<br>
				  <a href="${href}">${href}</a><br>
				  Your friend,<br>
				  Bob
				</p>
				"""
				.replace("${href}", h);
		var m = hm.replaceAll("<.*?>", "");
		IO.println("m=" + m);
//		mail(new Data(OffsetDateTime.now(), "foo.bar@example.com", "baz.qux@example.com", "Reset Your Password",
//				m, hm));
	}

	@Handle(method = "POST", path = "reset-password")
	public User resetPassword(String token, String password, String confirmPassword, CustomHttpExchange exchange) {
		if (token == null || token.isBlank() || password == null || password.isBlank()
				|| !password.equals(confirmPassword))
			throw new BadRequestException("Please correct invalid fields.");
		var u = crud().read(crud().find("resetPasswordToken", token));
		if (u != null && !Instant.now().isBefore(u.resetPasswordExpiration()))
			u = null;
		if (u == null)
			throw new ForbiddenException("Token is either invalid or has expired.");
		u = crud().update(u.id(), x -> x.withResetPassword(null, null).withPassword(password));
		var h = Map.of("alg", "HS256", "typ", "JWT");
		var p = Map.of("loggedInAs", u.email());
		var t = Jwt.generateToken(h, p, configuration.getProperty("blank-template.jwt.key"));
		exchange.setSessionCookie(t);
		return u;
	}

	@Override
	public User delete(Long id) {
		throw new RuntimeException();
	}

	@Handle(method = "DELETE", path = "(\\d+)")
	public User delete(Long id, CustomHttpExchange exchange) {
		var u = exchange.sessionUser();
		var x = super.delete(id);
		if (id == u.id())
			logout(exchange);
		return x;
	}

	@Override
	public List<User> delete(List<Long> ids) {
		throw new RuntimeException();
	}

	@Handle(method = "DELETE")
	public List<User> delete(@Bind("id") List<Long> ids, CustomHttpExchange exchange) {
		var u = exchange.sessionUser();
		var x = super.delete(ids);
		if (ids.contains(u.id()))
			logout(exchange);
		return x;
	}

	@Override
	protected Set<String> updateInclude(User entity) {
		var nn = Set.of("salt", "hash");
		return entity.salt() == null
				? Reflection.propertyNames(User.class).filter(x -> !nn.contains(x)).collect(Collectors.toSet())
				: null;
	}

//	private static void mail(Data d) {
//		SSLContext ssl;
//		try {
//			ssl = SSLContext.getInstance("TLSv1.3");
//			ssl.init(null, null, null);
//		} catch (GeneralSecurityException e) {
//			throw new RuntimeException(e);
//		}
//		try (var ch = SocketChannel.open()) {
//			var isa = new InetSocketAddress(InetAddress.getByName("smtp.example.com"), 465);
//			ch.connect(isa);
//			var se = ssl.createSSLEngine();
//			se.setUseClientMode(true);
//			try (var sch = new SslByteChannel(ch, se)) {
//				var hostname = InetAddress.getLocalHost().getCanonicalHostName();
//				var username = "foo.bar@example.com";
//				var password = "**********";
//				var b = "--=_Part_" + ThreadLocalRandom.current().ints(24, '0', '9' + 1).mapToObj(Character::toString)
//						.collect(Collectors.joining());
//				var dbuf = ByteBuffer.allocateDirect(8 * 1024);
//				var charset = Charset.forName("US-ASCII");
//				var encoder = charset.newEncoder();
//				var decoder = charset.newDecoder();
//				for (var s : new String[] { null, "EHLO " + hostname, "AUTH LOGIN",
//						Base64.getEncoder().encodeToString(username.getBytes()),
//						Base64.getEncoder().encodeToString(password.getBytes()), "MAIL FROM:<" + d.from + ">",
//						"RCPT TO:<" + d.to + ">", "DATA",
//						"Date: " + d.date.format(DateTimeFormatter.RFC_1123_DATE_TIME) + "\nFrom: " + d.from + "\nTo: "
//								+ d.to + "\nMessage-ID: <"
//								+ ThreadLocalRandom.current().ints(25, '0', '9' + 1).mapToObj(Character::toString)
//										.collect(Collectors.joining(""))
//								+ "@" + hostname + ">\nSubject: " + d.subject
//								+ "\nMIME-Version: 1.0\nContent-Type: multipart/alternative; boundary=\"" + b + "\"\n--"
//								+ b
//								+ "\nContent-Type: text/plain; charset=us-ascii\nContent-Transfer-Encoding: 7bit\n\n"
//								+ d.message + "--" + b
//								+ "\nContent-Type: text/html; charset=utf-8\nContent-Transfer-Encoding: 7bit\n\n"
//								+ d.htmlMessage + "--" + b + "--\n.",
//						"QUIT" }) {
//					if (s != null) {
//						IO.println("C: " + s.replace("\n", "\nC: "));
//						sch.write(encoder.encode(CharBuffer.wrap(s.replace("\n", "\r\n") + "\r\n")));
//					}
//					dbuf.clear();
//					sch.read(dbuf);
//					dbuf.flip();
//					var cb = decoder.decode(dbuf);
//					IO.println("\t" + cb.toString().replace("\n", "\n\t"));
//				}
//			}
//		} catch (IOException e) {
//			throw new UncheckedIOException(e);
//		}
//	}
//
//	private record Data(OffsetDateTime date, String from, String to, String subject, String message,
//			String htmlMessage) {
//	}
}
