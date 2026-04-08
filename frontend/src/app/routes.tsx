import { createBrowserRouter } from "react-router";
import { Root } from "./components/Root";
import { Home } from "./components/pages/Home";
import { ProjectList } from "./components/pages/ProjectList";
import { ProjectDetail } from "./components/pages/ProjectDetail";
import { ProjectCreate } from "./components/pages/ProjectCreate";
import { DeveloperList } from "./components/pages/DeveloperList";
import { DeveloperProfile } from "./components/pages/DeveloperProfile";
import { Login } from "./components/pages/Login";
import { NotFound } from "./components/pages/NotFound";

export const router = createBrowserRouter([
  {
    path: "/",
    Component: Root,
    children: [
      { index: true, Component: Home },
      { path: "projects", Component: ProjectList },
      { path: "projects/:id", Component: ProjectDetail },
      { path: "projects/new", Component: ProjectCreate },
      { path: "developers", Component: DeveloperList },
      { path: "developers/:id", Component: DeveloperProfile },
      { path: "login", Component: Login },
      { path: "*", Component: NotFound },
    ],
  },
]);
