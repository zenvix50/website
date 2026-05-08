import { Helmet } from 'react-helmet-async';
import PageWrapper from '../components/layout/PageWrapper';
import HeroSection from '../components/home/HeroSection';
import StatsBar from '../components/home/StatsBar';
import ServicesGrid from '../components/home/ServicesGrid';
import FeaturesSection from '../components/home/FeaturesSection';
import TerminalPreview from '../components/home/TerminalPreview';
import DashboardMockup from '../components/home/DashboardMockup';
import CTASection from '../components/home/CTASection';

export default function Home() {
  return (
    <PageWrapper>
      <Helmet>
        <title>zenviX — Dev Environment Manager | Home</title>
        <meta name="description" content="zenviX is a free, open-source local developer environment manager for Windows, macOS, and Linux." />
        <meta property="og:title" content="zenviX — Dev Environment Manager" />
        <meta property="og:description" content="Start, stop, and monitor every local service from one dashboard." />
        <meta property="og:image" content="/zenvix-og.png" />
        <meta property="og:type" content="website" />
        <meta name="twitter:card" content="summary_large_image" />
      </Helmet>
      <HeroSection />
      <StatsBar />
      <ServicesGrid />
      <FeaturesSection />
      <TerminalPreview />
      <DashboardMockup />
      <CTASection />
    </PageWrapper>
  );
}
