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
import WebComponent from "./web-component.js";

const adminRegex = /^\/admin(\/.*)?$/;

export default class Root extends WebComponent {

	static get templateNames() {
		return ["root"];
	}

	constructor() {
		super();
	}

	connectedCallback() {
		super.connectedCallback();
		this.addEventListener("click", this.handleClick);
		addEventListener("popstate", this.handlePopState);
	}

	disconnectedCallback() {
		super.disconnectedCallback();
		this.removeEventListener("click", this.handleClick);
		removeEventListener("popstate", this.handlePopState);
	}

	handleClick = event => {
		const a = event.target.closest("a");
		if (!a?.href || event.defaultPrevented || a.target)
			return;
		const u = new URL(a.href);
		if (!u.pathname.match(adminRegex) !== !location.pathname.match(adminRegex))
			return;
		event.preventDefault();
		history.pushState(undefined, "", u.pathname + u.search);
		dispatchEvent(new CustomEvent("popstate"));
	}

	handlePopState = () => {
		window.scrollTo(0, 0);
		delete this.state.notFound;
		this.requestDisplay();
	}

	async computeState() {
		const s = this.state;
		const nn = ["user"];
		nn.forEach(x => delete s[x]);
		const kkvv = await Promise.all(nn.map(x => this.fetchData(`/${x === "user" ? "users/me" : x}`).then(y => ([x, y]))));
		for (const [k, v] of kkvv)
			s[k] = v;
		if (s.user)
			s.user.roles = [{ name: "ADMIN" }];
		this.requestDisplay();
	}

	async updateDisplay() {
		const s = this.state;
		s.computeState ??= this.computeState();

		const m = location.pathname.match(adminRegex);
		if (m) {
			this.appendChild(this.interpolateDom({
				$template: "",
				admin: {
					$template: "admin",
					email: this.state?.user?.email,
					path: m[1] ?? "/"
				}
			}));
			return;
		}
		this.appendChild(this.interpolateDom({
			$template: "",
			content: s.notFound ? { $template: "not-found" } : {
				$template: "page",
				slug: (() => {
					const s2 = location.pathname.substring(1);
					return s2 ? s2 : "home";
				})()
			}
		}));
	}

	async fetchData(key) {
		/*
		if (Object.hasOwn(this.#serverData, key)) {
			const v = this.#serverData[key];
			delete this.#serverData[key];
			return v;
		}
		*/
		return await (await fetch(`${this.dataset.apiUrl}${key}`, { credentials: "include" })).json();
	}

	notFound() {
		this.state.notFound = true;
		this.requestDisplay();
	}
}
