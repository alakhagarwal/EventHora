import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach } from "vitest";
import EventForm from "@/components/EventForm";

const mockCreateEvent = vi.fn();
const mockUpdateEvent = vi.fn();
const mockPublishEvent = vi.fn();
const mockCancelEvent = vi.fn();
const mockUploadBanner = vi.fn();

vi.mock("@/lib/api", () => ({
  api: {
    createEvent: (...args: any[]) => mockCreateEvent(...args),
    updateEvent: (...args: any[]) => mockUpdateEvent(...args),
    publishEvent: (...args: any[]) => mockPublishEvent(...args),
    cancelEvent: (...args: any[]) => mockCancelEvent(...args),
    uploadBanner: (...args: any[]) => mockUploadBanner(...args),
  },
}));

const mockToastSuccess = vi.fn();
const mockToastError = vi.fn();
vi.mock("@/lib/toast", () => ({
  toast: {
    success: (...args: any[]) => mockToastSuccess(...args),
    error: (...args: any[]) => mockToastError(...args),
  },
}));

beforeEach(() => {
  vi.clearAllMocks();
  global.URL.createObjectURL = vi.fn(() => "blob:test-preview");
});

function fillRequiredFields() {
  fireEvent.change(screen.getByRole("textbox", { name: "Title" }), { target: { value: "Test Event" } });
  fireEvent.change(screen.getByLabelText("Event Date"), { target: { value: "2028-01-15" } });
  fireEvent.change(screen.getByLabelText("Registration Deadline"), { target: { value: "2028-01-10T18:00" } });
}

describe("EventForm — render", () => {
  it("renders all form sections", () => {
    render(<EventForm />);
    expect(screen.getByRole("heading", { name: "Basic details" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Banner" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Schedule" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Venue" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Capacity & pricing" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Important notes" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Contact" })).toBeInTheDocument();
  });

  it("shows 'Create Draft' button when no eventId", () => {
    render(<EventForm />);
    expect(screen.getByText("Create Draft")).toBeInTheDocument();
  });

  it("shows action buttons when eventId is provided", () => {
    render(<EventForm eventId="evt-123" />);
    expect(screen.getByText("Save Draft")).toBeInTheDocument();
    expect(screen.getByText("Publish")).toBeInTheDocument();
    expect(screen.getByText("Cancel Event")).toBeInTheDocument();
  });

  it("pre-fills fields from initial prop", () => {
    render(<EventForm initial={{ title: "Test Event", ticketPrice: 500, totalCapacity: 200 }} />);
    expect(screen.getByRole("textbox", { name: "Title" })).toHaveValue("Test Event");
    expect(screen.getByLabelText("Ticket Price")).toHaveValue(500);
    expect(screen.getByLabelText("Total Capacity")).toHaveValue(200);
  });
});

describe("EventForm — banner upload", () => {
  it("shows upload placeholder when no banner", () => {
    render(<EventForm />);
    expect(screen.getByText("Click to upload a banner image")).toBeInTheDocument();
  });

  it("shows preview when bannerUrl is provided via initial", () => {
    render(<EventForm initial={{ bannerUrl: "https://example.com/banner.jpg" }} />);
    expect(screen.getByRole("img")).toHaveAttribute("src", "https://example.com/banner.jpg");
    expect(screen.getByText("Change banner")).toBeInTheDocument();
  });

  it("uploads banner immediately when eventId exists", async () => {
    mockUploadBanner.mockResolvedValueOnce({ bannerUrl: "https://s3.example.com/new-banner.png" });

    render(<EventForm eventId="evt-123" />);

    const fileInput = screen.getByLabelText("Banner").querySelector('input[type="file"]')!;
    await userEvent.setup().upload(fileInput, new File(["content"], "test.png", { type: "image/png" }));

    await waitFor(() => {
      expect(mockUploadBanner).toHaveBeenCalledWith("evt-123", expect.any(File));
    });
  });

  it("shows blob preview when banner selected before event exists", async () => {
    render(<EventForm />);

    const fileInput = screen.getByLabelText("Banner").querySelector('input[type="file"]')!;
    await userEvent.setup().upload(fileInput, new File(["content"], "banner.png", { type: "image/png" }));

    expect(screen.getByRole("img")).toHaveAttribute("src", "blob:test-preview");
    expect(mockUploadBanner).not.toHaveBeenCalled();
  });

  it("uploads pending banner after create draft", async () => {
    mockCreateEvent.mockResolvedValueOnce({ id: "evt-new-1", bannerUrl: null });
    mockUploadBanner.mockResolvedValueOnce({ bannerUrl: "https://s3.example.com/uploaded.png" });

    render(<EventForm />);

    const fileInput = screen.getByLabelText("Banner").querySelector('input[type="file"]')!;
    await userEvent.setup().upload(fileInput, new File(["data"], "banner.png", { type: "image/png" }));

    fillRequiredFields();
    fireEvent.click(screen.getByText("Create Draft"));

    await waitFor(() => {
      expect(mockCreateEvent).toHaveBeenCalledTimes(1);
    });
    await waitFor(() => {
      expect(mockUploadBanner).toHaveBeenCalledWith("evt-new-1", expect.any(File));
    });
  });
});

describe("EventForm — create draft", () => {
  it("calls createEvent with form values and calls onSaved", async () => {
    const onSaved = vi.fn();
    mockCreateEvent.mockResolvedValueOnce({ id: "evt-456", bannerUrl: null });

    render(<EventForm onSaved={onSaved} />);

    fireEvent.change(screen.getByRole("textbox", { name: "Title" }), { target: { value: "My Event" } });
    fireEvent.change(screen.getByRole("textbox", { name: "Description" }), { target: { value: "Event description" } });
    fireEvent.change(screen.getByLabelText("Event Date"), { target: { value: "2028-06-15" } });
    fireEvent.change(screen.getByLabelText("Registration Deadline"), { target: { value: "2028-06-10T18:00" } });
    fireEvent.change(screen.getByLabelText("Ticket Price"), { target: { value: "1000" } });

    fireEvent.click(screen.getByText("Create Draft"));

    await waitFor(() => {
      expect(mockCreateEvent).toHaveBeenCalledWith(
        expect.objectContaining({ title: "My Event", description: "Event description", ticketPrice: 1000 })
      );
    });
    expect(onSaved).toHaveBeenCalledWith({ id: "evt-456", bannerUrl: null });
  });

  it("disables button while submitting", async () => {
    let resolve: (v: any) => void;
    mockCreateEvent.mockReturnValueOnce(new Promise((r) => { resolve = r; }));

    render(<EventForm />);
    fillRequiredFields();
    fireEvent.click(screen.getByText("Create Draft"));

    expect(screen.getByText("Creating…")).toBeInTheDocument();

    resolve!({ id: "evt-1", bannerUrl: null });
    await waitFor(() => {
      expect(screen.getByText("Save Draft")).toBeInTheDocument();
    });
  });
});

describe("EventForm — important notes", () => {
  it("adds and removes notes", async () => {
    render(<EventForm />);

    const noteInput = screen.getByPlaceholderText("Add a note…");
    await userEvent.setup().type(noteInput, "First note");
    fireEvent.click(screen.getByText("Add"));
    expect(screen.getByText("• First note")).toBeInTheDocument();

    await userEvent.setup().type(noteInput, "Second note");
    fireEvent.click(screen.getByText("Add"));
    expect(screen.getByText("• Second note")).toBeInTheDocument();

    fireEvent.click(screen.getAllByText("Remove")[0]);
    expect(screen.queryByText("• First note")).not.toBeInTheDocument();
    expect(screen.getByText("• Second note")).toBeInTheDocument();
  });
});

describe("EventForm — save draft & publish", () => {
  it("calls updateEvent on save draft", async () => {
    mockUpdateEvent.mockResolvedValueOnce({ id: "evt-1" });

    render(<EventForm eventId="evt-1" initial={{ title: "Existing" }} />);
    fireEvent.click(screen.getByText("Save Draft"));

    await waitFor(() => {
      expect(mockUpdateEvent).toHaveBeenCalledWith("evt-1", expect.objectContaining({ title: "Existing" }));
    });
  });

  it("calls publishEvent on publish", async () => {
    mockPublishEvent.mockResolvedValueOnce({ id: "evt-1" });

    render(<EventForm eventId="evt-1" />);
    fireEvent.click(screen.getByText("Publish"));

    await waitFor(() => {
      expect(mockPublishEvent).toHaveBeenCalledWith("evt-1");
    });
  });
});
