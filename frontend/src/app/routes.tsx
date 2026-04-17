/**
 * routes.tsx (전체 갱신판)
 */
import { createBrowserRouter } from "react-router";
import { Root } from "./components/Root";
import { Home } from "./components/pages/Home";
import { ProjectList } from "./components/pages/ProjectList";
import { ProjectDetail } from "./components/pages/ProjectDetail";
import { ProjectCreate } from "./components/pages/ProjectCreate";
import { ProjectEdit } from "./components/pages/ProjectEdit";
import { DeveloperList } from "./components/pages/DeveloperList";
import { DeveloperProfile } from "./components/pages/DeveloperProfile";
import { ClientList } from "./components/pages/ClientList";
import { Login } from "./components/pages/Login";
import { NotFound } from "./components/pages/NotFound";
import { ChatPage } from "./components/pages/ChatPage";
import { MyProposals } from "./components/pages/MyProposals";
import { MyPage } from "./components/pages/MyPage";
import { ProfileView } from "./components/pages/ProfileView";
import { MyProjects } from "./components/pages/MyProjects";
import { MyPortfolio } from "./components/pages/MyPortfolio";
import { AdminDashboard } from "./components/pages/AdminDashboard";
import { CsPage } from "./components/pages/CsPage";
import { SearchPage } from "./components/pages/SearchPage";
import { ClientDetail } from "./components/pages/ClientDetail";

export const router = createBrowserRouter([
  {
    path: "/",
    Component: Root,
    children: [
      { index: true, Component: Home },
      { path: "projects", Component: ProjectList },
      { path: "projects/new", Component: ProjectCreate },
      { path: "projects/:id", Component: ProjectDetail },
      { path: "projects/:id/edit", Component: ProjectEdit },
      { path: "developers", Component: DeveloperList },
      { path: "developers/:id", Component: DeveloperProfile },
      { path: "clients", Component: ClientList },
      { path: "clients/:id", Component: ClientDetail },
      { path: "login", Component: Login },
      { path: "chat", Component: ChatPage },
      { path: "cs", Component: CsPage },
      { path: "search", Component: SearchPage },
      { path: "my-proposals", Component: MyProposals },
      // ── 신규 라우트 ──
      { path: "profile", Component: ProfileView },
      { path: "my-profile", Component: MyPage },
      { path: "my-projects", Component: MyProjects },
      { path: "my-portfolio", Component: MyPortfolio },
      { path: "admin", Component: AdminDashboard },
      { path: "*", Component: NotFound },
    ],
  },
]);
