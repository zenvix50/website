import mongoose from 'mongoose';

const subscriberSchema = new mongoose.Schema({
  email: { type: String, required: true, unique: true, lowercase: true },
  subscribedAt: { type: Date, default: Date.now },
  notifyOnRelease: { type: Boolean, default: true },
});

export default mongoose.model('Subscriber', subscriberSchema);
