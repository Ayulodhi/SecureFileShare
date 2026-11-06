import { useState, useEffect } from 'react';
import { AuthProvider, useAuth } from './state/AuthContext';
import { Layout, NavTab } from './components/Layout';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import { FilesPage } from './pages/FilesPage';
import { UploadPage } from './pages/UploadPage';
import { SharesPage } from './pages/SharesPage';
import { AuditPage } from './pages/AuditPage';
import { KeysPage } from './pages/KeysPage';
import { ShareAccessPage } from './pages/ShareAccessPage';
import { ToastContainer } from './components/Toast';

function AppContent() {
  const { isAuthenticated } = useAuth();
  const [authView, setAuthView] = useState<'login' | 'register'>('login');
  const [currentTab, setCurrentTab] = useState<NavTab>('files');
  const [shareToken, setShareToken] = useState<string | null>(null);

  // Check initial URL path for /share/:token
  useEffect(() => {
    const path = window.location.pathname;
    if (path.startsWith('/share/')) {
      const token = path.replace('/share/', '').trim();
      if (token) {
        setShareToken(token);
      }
    }
  }, []);

  // Recipient public share download route
  if (shareToken) {
    return (
      <ShareAccessPage
        token={shareToken}
        onNavigateHome={() => {
          setShareToken(null);
          window.history.pushState({}, '', '/');
        }}
      />
    );
  }

  // Unauthenticated authentication screens
  if (!isAuthenticated) {
    return authView === 'login' ? (
      <LoginPage onNavigateToRegister={() => setAuthView('register')} />
    ) : (
      <RegisterPage onNavigateToLogin={() => setAuthView('login')} />
    );
  }

  // Authenticated Dashboard Layout
  return (
    <Layout currentTab={currentTab} onSelectTab={setCurrentTab}>
      {currentTab === 'files' && (
        <FilesPage onNavigateToUpload={() => setCurrentTab('upload')} />
      )}
      {currentTab === 'upload' && (
        <UploadPage onUploadSuccess={() => setCurrentTab('files')} />
      )}
      {currentTab === 'shares' && <SharesPage />}
      {currentTab === 'audit' && <AuditPage />}
      {currentTab === 'keys' && <KeysPage />}
    </Layout>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <AppContent />
      <ToastContainer />
    </AuthProvider>
  );
}
