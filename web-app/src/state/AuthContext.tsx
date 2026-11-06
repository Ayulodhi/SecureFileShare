import { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { getCurrentUser, getAuthToken, setCurrentUser, setAuthToken, UserSession } from '../api/client';
import { hasUserKeys } from '../crypto/keyVault';

interface AuthContextType {
  user: UserSession | null;
  token: string | null;
  isAuthenticated: boolean;
  hasKeys: boolean;
  login: (user: UserSession, token: string) => void;
  logout: () => void;
  refreshKeyStatus: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserSession | null>(getCurrentUser());
  const [token, setToken] = useState<string | null>(getAuthToken());
  const [hasKeys, setHasKeys] = useState<boolean>(false);

  const checkKeys = async (username?: string) => {
    const targetUser = username || user?.username;
    if (targetUser) {
      const exists = await hasUserKeys(targetUser);
      setHasKeys(exists);
    } else {
      setHasKeys(false);
    }
  };

  useEffect(() => {
    checkKeys();
  }, [user]);

  const login = (newUser: UserSession, newToken: string) => {
    setCurrentUser(newUser);
    setAuthToken(newToken);
    setUser(newUser);
    setToken(newToken);
    checkKeys(newUser.username);
  };

  const logout = () => {
    setCurrentUser(null);
    setAuthToken(null);
    setUser(null);
    setToken(null);
    setHasKeys(false);
  };

  const refreshKeyStatus = async () => {
    await checkKeys();
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAuthenticated: !!token && !!user,
        hasKeys,
        login,
        logout,
        refreshKeyStatus,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
