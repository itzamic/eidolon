import { render, screen } from '@testing-library/react';
import StatCard from '@/components/StatCard';

describe('StatCard', () => {
  it('renders label and value', () => {
    render(<StatCard label="CPU" value="42%" />);
    expect(screen.getByText('CPU')).toBeInTheDocument();
    expect(screen.getByText('42%')).toBeInTheDocument();
  });

  it('renders optional sub text when provided', () => {
    render(<StatCard label="Heap" value="1.0 GB" sub="Max 2.0 GB" />);
    expect(screen.getByText('Max 2.0 GB')).toBeInTheDocument();
  });

  it('does not render sub when not provided', () => {
    render(<StatCard label="Threads" value="20" />);
    expect(screen.queryByText(/Max/)).toBeNull();
  });
});
