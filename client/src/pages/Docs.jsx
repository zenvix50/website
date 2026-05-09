import { useState } from 'react';
import { Helmet } from 'react-helmet-async';
import PageWrapper from '../components/layout/PageWrapper';
import DocsSidebar from '../components/docs/DocsSidebar';
import DocsContent from '../components/docs/DocsContent';

export default function Docs() {
  const [active, setActive] = useState('Quick Start');

  return (
    <PageWrapper>
      <Helmet>
        <title>zenviX — Dev Environment Manager | Docs</title>
        <meta name="description" content="Complete documentation for zenviX — setup, services, configuration, and API reference." />
      </Helmet>

      <div className="max-w-7xl mx-auto px-4 py-12">
        <div className="flex gap-12">
          <DocsSidebar active={active} onSelect={setActive} />
          <DocsContent active={active} />
        </div>
      </div>
    </PageWrapper>
  );
}
