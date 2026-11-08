import { KeycloakTokenParsed } from "keycloak-js";

export interface ExtendedKeycloakTokenParsed extends KeycloakTokenParsed {
  preferred_username?: string;
  email?: string;
  given_name?: string;
  family_name?: string;
  realm_access?: {
    roles: string[];
  };
  resource_access?: {
    [key: string]: {
      roles: string[];
    };
  };
}
