import Subscriber from '../models/Subscriber.js';

function isValidEmail(email) {
  if (typeof email !== 'string' || email.length > 254 || email.length < 5) return false;
  const at = email.indexOf('@');
  if (at === -1 || at === 0) return false;
  const dot = email.lastIndexOf('.');
  return dot > at + 1 && dot < email.length - 1 && !email.includes(' ');
}

export const subscribe = async (req, res) => {
  try {
    const { email } = req.body;
    if (!email || !isValidEmail(email)) {
      return res.status(400).json({ message: 'Invalid email' });
    }

    await Subscriber.findOneAndUpdate(
      { email },
      { email, notifyOnRelease: true },
      { upsert: true, new: true }
    );

    res.json({ message: 'Subscribed successfully!' });
  } catch (err) {
    if (err.code === 11000) return res.json({ message: 'Already subscribed!' });
    res.status(500).json({ message: err.message });
  }
};
